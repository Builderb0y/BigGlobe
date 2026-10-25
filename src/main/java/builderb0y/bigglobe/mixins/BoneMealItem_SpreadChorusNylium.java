package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BoneMealItem.class)
public class BoneMealItem_SpreadChorusNylium {

	@Inject(method = "growCrop", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_spreadChorusNylium(ItemStack stack, Level world, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
		BlockState state = world.getBlockState(pos);
		if (state.is(Blocks.END_STONE)) {
			BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
			for (mutablePos.setY(pos.getY() - 1); mutablePos.getY() <= pos.getY() + 1; mutablePos.setY(mutablePos.getY() + 1)) {
				for (mutablePos.setZ(pos.getZ() - 1); mutablePos.getZ() <= pos.getZ() + 1; mutablePos.setZ(mutablePos.getZ() + 1)) {
					for (mutablePos.setX(pos.getX() - 1); mutablePos.getX() <= pos.getX() + 1; mutablePos.setX(mutablePos.getX() + 1)) {
						if (mutablePos.getX() == pos.getX() && mutablePos.getY() == pos.getY() && mutablePos.getZ() == pos.getZ()) continue;
						if (world.getBlockState(mutablePos).is(BigGlobeBlockTags.END_STONE_SPREADABLE)) {
							world.setBlockAndUpdate(pos, BigGlobeBlocks.OVERGROWN_END_STONE.defaultBlockState());
							callback.setReturnValue(Boolean.TRUE);
							return;
						}
					}
				}
			}
		}
	}
}