package torcherino.blocks;

import net.minecraft.block.BlockTorchWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.INameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import torcherino.blocks.miscellaneous.TorcherinoTileEntity;
import javax.annotation.Nullable;

public class TorcherinoWallBlock extends BlockTorchWall
{
	protected TorcherinoWallBlock(){ super(Properties.from(Blocks.WALL_TORCH)); }

	@Override public boolean hasTileEntity(IBlockState state)
	{
		return true;
	}

	@Nullable @Override public TileEntity createTileEntity(IBlockState state, IBlockReader world)
	{
		return new TorcherinoTileEntity();
	}

	@Override public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (world.isRemote || hand == EnumHand.OFF_HAND) return true;
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof TorcherinoTileEntity)) return true;
		NetworkHooks.openGui((EntityPlayerMP) player, (TorcherinoTileEntity) tile, pos);
		return true;
	}

	@Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, @Nullable EntityLivingBase placer, ItemStack stack)
	{
		if (stack.hasDisplayName())
		{
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof TorcherinoTileEntity)) return;
			((TorcherinoTileEntity) tile).setCustomName(stack.getDisplayName());
		}
	}

	@Override public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack)
	{
		if (te instanceof INameable && ((INameable) te).hasCustomName())
		{
			player.addStat(StatList.BLOCK_MINED.get(this));
			player.addExhaustion(0.005F);
			if (world.isRemote) return;
			int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);
			Item item = this.getItemDropped(state, world, pos, fortune).asItem();
			if (item == Items.AIR) return;
			int itemsDropped = this.getItemsToDropCount(state, fortune, world, pos, world.rand);
			ItemStack itemstack = new ItemStack(item, itemsDropped);
			itemstack.setDisplayName(((INameable) te).getCustomName());
			spawnAsEntity(world, pos, itemstack);
		}
		else
		{
			super.harvestBlock(world, player, pos, state, null, stack);
		}
	}
}
