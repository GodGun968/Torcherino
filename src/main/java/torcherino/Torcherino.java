package torcherino;

import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import torcherino.api.TorcherinoAPI;
import torcherino.blocks.Blocks;
import torcherino.config.Config;
import torcherino.network.Networker;

@Mod(Torcherino.MOD_ID)
public class Torcherino
{
    public static final Logger LOGGER = LogManager.getLogger(Torcherino.class);
    public static final String MOD_ID = "torcherino";

    public Torcherino()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.initialise();
        Blocks.INSTANCE.initialise();
        Networker.INSTANCE.initialise();
        eventBus.register(Blocks.INSTANCE);
        eventBus.addListener(this::processIMC);
        MinecraftForge.EVENT_BUS.addListener(this::processPlayerJoin);
        TorcherinoAPI.INSTANCE.blacklistBlock(net.minecraft.block.Blocks.WATER);
        TorcherinoAPI.INSTANCE.blacklistBlock(net.minecraft.block.Blocks.LAVA);
        TorcherinoAPI.INSTANCE.blacklistBlock(net.minecraft.block.Blocks.AIR);
        TorcherinoAPI.INSTANCE.blacklistBlock(net.minecraft.block.Blocks.CAVE_AIR);
        TorcherinoAPI.INSTANCE.blacklistBlock(net.minecraft.block.Blocks.VOID_AIR);
    }

    public static ResourceLocation resloc(String path) { return new ResourceLocation(MOD_ID, path); }

    private void processPlayerJoin(final PlayerEvent.PlayerLoggedInEvent event) { Networker.INSTANCE.sendServerTiers((ServerPlayerEntity) event.getPlayer()); }

    @SubscribeEvent
    public void processIMC(final InterModProcessEvent event)
    {
        // To use: in InterModEnqueueEvent call
        // InterModComms.sendTo( MOD_ID, Method , supplier);
        // See processMessage method below for method and what they take
        event.getIMCStream().forEach(this::processMessage);
    }

    private void processMessage(final InterModComms.IMCMessage message)
    {
        String method = message.getMethod();
        Object value = message.getMessageSupplier().get();
        if (method.equals("blacklist_block"))
        {
            if (value instanceof ResourceLocation) { TorcherinoAPI.INSTANCE.blacklistBlock((ResourceLocation) value); }
            else if (value instanceof Block) TorcherinoAPI.INSTANCE.blacklistBlock((Block) value);
        }
        else if (method.equals("blacklist_tile"))
        {
            if (value instanceof ResourceLocation) { TorcherinoAPI.INSTANCE.blacklistTileEntity((ResourceLocation) value); }
            else if (value instanceof TileEntityType) TorcherinoAPI.INSTANCE.blacklistTileEntity((TileEntityType) value);
        }
    }

    /*
     * TODO: Move over to using https://mcforge.readthedocs.io/en/1.13.x/gettingstarted/dependencymanagement/
     * TODO: Finally release the mod?
     * TODO: Add logging to api.
     */
}