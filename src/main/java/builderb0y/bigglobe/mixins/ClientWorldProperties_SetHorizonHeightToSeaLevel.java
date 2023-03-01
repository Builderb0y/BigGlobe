package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.HeightLimitView;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.settings.OverworldClientSettings;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.Properties.class)
public class ClientWorldProperties_SetHorizonHeightToSeaLevel {

	@Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
	private void bigglobe_modifySkyDarknessHeight(HeightLimitView world, CallbackInfoReturnable<Double> callback) {
		OverworldClientSettings settings = ClientState.settings;
		if (settings != null) {
			callback.setReturnValue((double)(settings.sea_level()));
		}
	}
}