package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.blocks.RiverWaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

@Mixin(FlowingFluid.class)
public class FlowableFluid_DontFlowInRivers {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void bigglobe_dontFlowInRivers(
		Level world,
		BlockPos pos,
		FluidState fluidState,
		CallbackInfo callback
	) {
		if (world.getBlockState(pos).getBlock() instanceof RiverWaterBlock) {
			callback.cancel();
		}
	}
}
