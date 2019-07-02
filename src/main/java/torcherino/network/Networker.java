package torcherino.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import torcherino.Torcherino;
import torcherino.api.TorcherinoAPI;
import torcherino.api.blocks.TorcherinoTileEntity;

public class Networker
{
	public static final Networker INSTANCE = new Networker();

	public SimpleChannel torcherinoChannel;

	public void initialise()
	{
		torcherinoChannel = NetworkRegistry.newSimpleChannel(Torcherino.resloc("channel"), () -> "2", version -> version.equals("2"), version -> version.equals("2"));
		torcherinoChannel.registerMessage(0, ValueUpdateMessage.class, ValueUpdateMessage::encode, ValueUpdateMessage::decode, ValueUpdateMessage::handle);
		torcherinoChannel.registerMessage(1, OpenScreenMessage.class, OpenScreenMessage::encode, OpenScreenMessage::decode, OpenScreenMessage::handle);
		torcherinoChannel.registerMessage(2, S2CTierSyncMessage.class, S2CTierSyncMessage::encode, S2CTierSyncMessage::decode, S2CTierSyncMessage::handle);
	}

	public boolean openScreenServer(World world, EntityPlayer player, BlockPos pos)
	{
		if (world.isRemote) return true;
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TorcherinoTileEntity)) return true;
		torcherinoChannel.sendTo(((TorcherinoTileEntity) tile).createOpenMessage(), ((EntityPlayerMP) player).connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
		return true;
	}

	public void sendServerTiers(EntityPlayerMP player)
	{
		S2CTierSyncMessage message = new S2CTierSyncMessage(TorcherinoAPI.INSTANCE.getTiers());
		torcherinoChannel.sendTo(message, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
	}
}