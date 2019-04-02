package torcherino.Blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;
import torcherino.Blocks.Tiles.TileEntityTorcherino;
import torcherino.Utils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@SuppressWarnings("deprecation")
public class BlockTorcherinoWall extends BlockTorchWall
{
	private Block BASE;
	BlockTorcherinoWall(Block base) { super(Properties.from(base)); this.BASE = base; }

	@Override public boolean hasTileEntity(IBlockState state){ return true; }
	@Override public TileEntity createTileEntity(IBlockState state, IBlockReader world){ return BASE.createTileEntity(state, world); }
	@Override @ParametersAreNonnullByDefault public void onBlockAdded(IBlockState state, World world, BlockPos blockPos, IBlockState oldState){ neighborChanged(state, world, blockPos, null, null); }
	@Override @Nonnull public EnumPushReaction getPushReaction(@Nonnull IBlockState state){ return EnumPushReaction.IGNORE; }

	@Override @ParametersAreNonnullByDefault public void neighborChanged(IBlockState selfState, World world, BlockPos selfPos, Block neighborBlock, BlockPos neighborPos)
	{
		if (world.isRemote) return;
		TileEntity tileEntity = world.getTileEntity(selfPos);
		if (tileEntity == null) return;
		EnumFacing oppositeFacing = selfState.get(HORIZONTAL_FACING).getOpposite();
		((TileEntityTorcherino) tileEntity).setPoweredByRedstone(world.isSidePowered(selfPos.offset(oppositeFacing), oppositeFacing));
	}

	@Override @ParametersAreNonnullByDefault public void tick(IBlockState state, World world, BlockPos pos, Random random)
	{
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof ITickable) ((ITickable) tileEntity).tick();
	}

	@Override @ParametersAreNonnullByDefault public void onReplaced(IBlockState state, World world, BlockPos pos, IBlockState state1, boolean b)
	{
		TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity != null) tileEntity.remove();
	}

	@Override @ParametersAreNonnullByDefault public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack)
	{
		if (world.isRemote || !Utils.logPlacement) return;
		String prefix = "Something";
		if (placer != null) prefix = placer.getDisplayName().getString() + "(" + placer.getCachedUniqueIdString() + ")";
		Utils.LOGGER.info("[Torcherino] {} placed a {} at {} {} {}.", prefix, StringUtils.capitalize(getTranslationKey().replace("block.torcherino.", "").replace("_", " ")), pos.getX(), pos.getY(), pos.getZ());
	}

	@Override @ParametersAreNonnullByDefault public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (world.isRemote || hand == EnumHand.OFF_HAND) return true;
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TileEntityTorcherino)) return true;
		TileEntityTorcherino torch = (TileEntityTorcherino) tile;
		torch.changeMode(Utils.keyStates.getOrDefault(player, false));
		player.sendStatusMessage(torch.getDescription(), true);
		return true;
	}
}