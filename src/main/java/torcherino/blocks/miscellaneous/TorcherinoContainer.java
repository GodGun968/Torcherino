package torcherino.blocks.miscellaneous;

import io.netty.buffer.Unpooled;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import torcherino.Utilities;
import torcherino.network.Networker;

public class TorcherinoContainer extends Container
{
	// This class is simply for wrapping the tileEntity and storing variables
	private int speed, xRange, zRange, yRange, redstoneMode;
	private boolean needsSyncing;
	private final ITextComponent displayName;
	private final TorcherinoTileEntity tileEntity;

	public TorcherinoContainer(TorcherinoTileEntity tileEntity, ITextComponent displayName)
	{
		this.tileEntity = tileEntity;
		this.displayName = displayName;
		this.needsSyncing = false;
	}

	@Override public boolean canInteractWith(EntityPlayer playerIn)
	{
		return true;
	}

	public TorcherinoTileEntity getTileEntity()
	{
		return this.tileEntity;
	}

	public void setSpeed(int speed)
	{
		this.speed = speed;
		needsSyncing = true;
	}

	public void setXRange(int xRange)
	{
		this.xRange = xRange;
		needsSyncing = true;
	}

	public void setZRange(int zRange)
	{
		this.zRange = zRange;
		needsSyncing = true;
	}

	public void setYRange(int yRange)
	{
		this.yRange = yRange;
		needsSyncing = true;
	}

	public void setRedstoneMode(int redstoneMode)
	{
		this.redstoneMode = redstoneMode;
		needsSyncing = true;
	}

	public int getXRange(){ return xRange; }

	public int getZRange(){ return zRange; }

	public int getYRange(){ return yRange; }

	public int getMaxXZRange(){ return tileEntity.getTier().XZ_RANGE; }

	public int getMaxYRange(){ return tileEntity.getTier().Y_RANGE; }

	public int getSpeed(){ return speed; }

	public int getMaxSpeed(){ return tileEntity.getTier().MAX_SPEED; }

	public int getRedstoneMode(){ return redstoneMode; }

	public ITextComponent getDisplayName(){ return displayName; }

	@Override public void onContainerClosed(EntityPlayer player)
	{
		// Lets inform the server of the variables
		// needsSyncing should only be true on the client but lets check just incase
		if (needsSyncing && player instanceof EntityPlayerSP)
		{
			Utilities.LOGGER.info("Sending data to server.");
			Networker.ValueUpdateMessage message = new Networker.ValueUpdateMessage(tileEntity.getPos(), getXRange(), getZRange(), getYRange(), getSpeed(), getRedstoneMode(), new PacketBuffer(Unpooled.buffer()));
			Networker.INSTANCE.torcherinoChannel.sendToServer(message);
		}
		super.onContainerClosed(player);
	}
}
