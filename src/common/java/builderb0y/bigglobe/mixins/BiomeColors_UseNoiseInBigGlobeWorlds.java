package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.ClientState;

@Mixin(BiomeColors.class)
@Environment(EnvType.CLIENT)
public class BiomeColors_UseNoiseInBigGlobeWorlds {

	@Inject(method = "getAverageColor", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_overrideColor(BlockAndTintGetter world, BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> callback) {
		ClientState.overrideColor(world, pos.getX(), pos.getY(), pos.getZ(), resolver, callback);
	}
}