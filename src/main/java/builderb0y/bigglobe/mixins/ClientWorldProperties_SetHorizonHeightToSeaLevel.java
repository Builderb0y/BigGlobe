package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.mixinInterfaces.DimensionalBlockView;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.Properties.class)
public class ClientWorldProperties_SetHorizonHeightToSeaLevel {

	@Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
	private void bigglobe_modifySkyDarknessHeight(HeightLimitView world, CallbackInfoReturnable<Double> callback) {
		RegistryKey<World> dimension = world instanceof DimensionalBlockView dimensional ? dimensional.bigglobe_getDimension() : null;
		if (dimension == null) return;
		ClientState state = ClientState.get(dimension);
		if (state == null) return;
		ClientGeneratorParams params = state.generatorParams;
		if (params == null) return;
		if (params.seaLevel == null) return;
		callback.setReturnValue(params.seaLevel.doubleValue());
	}
}