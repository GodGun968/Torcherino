package torcherino.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import torcherino.api.Tier;
import torcherino.api.TierSupplier;
import torcherino.api.TorcherinoAPI;
import torcherino.config.Config;
import torcherino.platform.NetworkUtils;

import java.util.HashSet;
import java.util.Set;

public class TorcherinoBlockEntity extends BlockEntity implements Nameable, TierSupplier {
    private static final int TICKS_PER_SECONDS = 20;
    private static final int SECONDS_PER_UPDATE = 5;
    public int randomTicks;
    private Component customName;
    private int xRange, yRange, zRange, speed, redstoneMode;
    private boolean areSettingsValid = false;
    private Set<TickableBlock> tickableBlockCache = new HashSet<>();
    private long lastCacheUpdate = -TICKS_PER_SECONDS * SECONDS_PER_UPDATE * 2;
    private boolean active;
    private ResourceLocation tierID;
    private String uuid = "";

    public TorcherinoBlockEntity(BlockPos pos, BlockState state) {
        super(Registry.BLOCK_ENTITY_TYPE.get(new ResourceLocation("torcherino", "torcherino")), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TorcherinoBlockEntity entity) {
        if (entity.active && entity.areSettingsValid && level instanceof ServerLevel serverLevel) {
            if (!Config.INSTANCE.online_mode.equals("") && !NetworkUtils.getInstance().s_isPlayerOnline(entity.getOwner())) {
                return;
            }
            if (level.getGameTime() - entity.lastCacheUpdate > SECONDS_PER_UPDATE * TICKS_PER_SECONDS) {
                entity.tickableBlockCache.clear();
                for (BlockPos blockPos : BlockPos.betweenClosed(pos.getX() - entity.xRange, pos.getY() - entity.yRange, pos.getZ() - entity.zRange, pos.getX() + entity.xRange, pos.getY() + entity.yRange, pos.getZ() + entity.zRange)) {
                    var blockState = level.getBlockState(blockPos);
                    var block = blockState.getBlock();
                    if (TorcherinoAPI.INSTANCE.isBlockBlacklisted(block)) {

                    } else if (block instanceof EntityBlock eBlock) {
                        var blockEntity = level.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            if (!TorcherinoAPI.INSTANCE.isBlockEntityBlacklisted(blockEntity.getType())) {
                                var ticker = (BlockEntityTicker<BlockEntity>) eBlock.getTicker(level, blockState, blockEntity.getType());
                                entity.tickableBlockCache.add(new TickableBlock(blockPos, ticker));
                            }
                        }
                    } else {
                        entity.tickableBlockCache.add(new TickableBlock(blockPos, null));
                    }
                }
                // todo: get on load and then when updated
                entity.randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
                entity.lastCacheUpdate = level.getGameTime();
            }
            for (TickableBlock tickableBlock : entity.tickableBlockCache) {
                entity.tickBlock(serverLevel, tickableBlock, Mth.clamp(4096 / (entity.speed * Config.INSTANCE.random_tick_rate), 1, 4096));
            }
        }

    }

    private void tickBlock(ServerLevel level, TickableBlock tickableBlock, int scaledTickRate) {
        BlockState state = level.getBlockState(tickableBlock.pos);
        Block block = state.getBlock();
        // todo: should I special code checks for blocks like CropBLock which stop ticking once fully grown?
        if (block.isRandomlyTicking(state) && level.getRandom().nextInt(scaledTickRate) < randomTicks) {
            state.randomTick(level, tickableBlock.pos, level.getRandom());
        }
        if (tickableBlock.hasTicker()) {
            BlockEntity entity = level.getBlockEntity(tickableBlock.pos);
            if (entity == null || entity.isRemoved()) {
                tickableBlockCache.remove(tickableBlock);
            } else {
                for (int i = 0; i < speed; i++) {
                    if (entity.isRemoved()) {
                        tickableBlockCache.remove(tickableBlock);
                        break;
                    }
                    // invalid, tickableBlock.hasTicker checks for this.
                    //noinspection ConstantConditions
                    tickableBlock.ticker.tick(level, tickableBlock.pos, state, entity);
                }
            }
        }
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Override
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(Component name) {
        customName = name;
    }

    private String getOwner() {
        return uuid;
    }

    public void setOwner(String s) {
        uuid = s;
    }

    @Override
    public Component getName() {
        return hasCustomName() ? customName : new TranslatableComponent(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> getBlockState().neighborChanged(level, worldPosition, null, null, false)));
        }
    }

    public boolean readClientData(int xRange, int zRange, int yRange, int speed, int redstoneMode) {
        Tier tier = TorcherinoAPI.INSTANCE.getTiers().get(getTier());
        if (this.valueInRange(xRange, 0, tier.xzRange()) &&
                this.valueInRange(zRange, 0, tier.xzRange()) &&
                this.valueInRange(yRange, 0, tier.yRange()) &&
                this.valueInRange(speed, 0, tier.maxSpeed()) &&
                this.valueInRange(redstoneMode, 0, 3)) {
            this.xRange = xRange;
            this.zRange = zRange;
            this.yRange = yRange;
            this.speed = speed;
            this.redstoneMode = redstoneMode;
            this.areSettingsValid = true;
            this.getBlockState().neighborChanged(level, worldPosition, null, null, false);
            return true;
        }
        return false;
    }

    private boolean valueInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    @Override
    public ResourceLocation getTier() {
        if (tierID == null) {
            Block block = this.getBlockState().getBlock();
            if (block instanceof TierSupplier supplier) {
                tierID = supplier.getTier();
            }
        }
        return tierID;
    }

    public void setPoweredByRedstone(boolean powered) {
        switch (redstoneMode) {
            case 0 -> active = !powered;
            case 1 -> active = powered;
            case 2 -> active = true;
            case 3 -> active = false;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        if (this.hasCustomName()) {
            tag.putString("CustomName", Component.Serializer.toJson(getCustomName()));
        }
        tag.putInt("XRange", xRange);
        tag.putInt("ZRange", zRange);
        tag.putInt("YRange", yRange);
        tag.putInt("Speed", speed);
        tag.putInt("RedstoneMode", redstoneMode);
        tag.putBoolean("Active", active);
        tag.putString("Owner", getOwner() == null ? "" : getOwner());
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CustomName", 8)) {
            this.setCustomName(Component.Serializer.fromJson(tag.getString("CustomName")));
        }
        xRange = tag.getInt("XRange");
        zRange = tag.getInt("ZRange");
        yRange = tag.getInt("YRange");
        speed = tag.getInt("Speed");
        redstoneMode = tag.getInt("RedstoneMode");
        active = tag.getBoolean("Active");
        uuid = tag.getString("Owner");
        this.areSettingsValid = true;
    }

    public void openTorcherinoScreen(ServerPlayer player) {
        NetworkUtils.getInstance().s2c_openTorcherinoScreen(player, worldPosition, this.getName(), xRange, zRange, yRange, speed, redstoneMode);
    }

    private static record TickableBlock(@NotNull BlockPos pos, @Nullable BlockEntityTicker<BlockEntity> ticker) {
        public boolean hasTicker() {
            return ticker != null;
        }
    }
}
