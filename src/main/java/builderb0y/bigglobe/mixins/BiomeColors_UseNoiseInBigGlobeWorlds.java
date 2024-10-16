package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;

import builderb0y.bigglobe.ClientState;

@Mixin(BiomeColors.class)
@Environment(EnvType.CLIENT)
public class BiomeColors_UseNoiseInBigGlobeWorlds {

	@Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
	private static void bigglobe_overrideColor(BlockRenderView world, BlockPos pos, ColorResolver resolver, CallbackInfoReturnable<Integer> callback) {
		ClientState.overrideColor(pos.getX(), pos.getY(), pos.getZ(), resolver, callback);
	}
}