package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.util.DimensionalBlockView;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.ClientLevelData.class)
public class ClientWorldProperties_SetHorizonHeightToSeaLevel {

	@Inject(method = "getHorizonHeight", at = @At("HEAD"), cancellable = true)
	private void bigglobe_modifySkyDarknessHeight(LevelHeightAccessor world, CallbackInfoReturnable<Double> callback) {
		ResourceKey<Level> dimension = world instanceof BlockGetter blockView ? DimensionalBlockView.getDimension(blockView) : null;
		if (dimension == null) return;
		ClientState state = ClientState.get(dimension);
		if (state == null) return;
		ClientGeneratorParams params = state.generatorParams;
		if (params == null) return;
		if (params.seaLevel == null) return;
		callback.setReturnValue(params.seaLevel.doubleValue());
	}
}