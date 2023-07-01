package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;

@Mixin(BoneMealItem.class)
public class BoneMealItem_SpreadChorusNylium {

	@Inject(method = "useOnFertilizable", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_spreadChorusNylium(ItemStack stack, World world, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
		BlockState state = world.getBlockState(pos);
		if (state.isOf(Blocks.END_STONE)) {
			BlockPos.Mutable mutablePos = new BlockPos.Mutable();
			for (mutablePos.setY(pos.getY() - 1); mutablePos.getY() <= pos.getY() + 1; mutablePos.setY(mutablePos.getY() + 1)) {
				for (mutablePos.setZ(pos.getZ() - 1); mutablePos.getZ() <= pos.getZ() + 1; mutablePos.setZ(mutablePos.getZ() + 1)) {
					for (mutablePos.setX(pos.getX() - 1); mutablePos.getX() <= pos.getX() + 1; mutablePos.setX(mutablePos.getX() + 1)) {
						if (mutablePos.getX() == pos.getX() && mutablePos.getY() == pos.getY() && mutablePos.getZ() == pos.getZ()) continue;
						if (world.getBlockState(mutablePos).isIn(BigGlobeBlockTags.END_STONE_SPREADABLE)) {
							world.setBlockState(pos, BigGlobeBlocks.OVERGROWN_END_STONE.getDefaultState());
							callback.setReturnValue(Boolean.TRUE);
							return;
						}
					}
				}
			}
		}
	}
}