package builderb0y.bigglobe.mixins;

import java.util.Map;
import java.util.stream.Collectors;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@Mixin(targets = "net.minecraft.server.dedicated.DedicatedServerProperties$WorldDimensionData")
public class WorldGenProperties_LogLevelType {

	@Shadow
	@Final
	private String levelType;

	@Inject(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"))
	private void bigglobe_logLevelType(
		Provider registries,
		CallbackInfoReturnable<WorldDimensions> callback,
		@Local(index = 4) Holder<WorldPreset> preset
	) {
		BigGlobeMod.LOGGER.info(
			"The level-type is "
			+ this.levelType
			+ ". The world preset has the following dimensions: "
			+ ((WorldPreset_DimensionsAccess)(preset.value()))
			.bigglobe_getDimensions()
			.entrySet()
			.stream()
			.map((Map.Entry<ResourceKey<LevelStem>, LevelStem> mapEntry) ->
				mapEntry.getKey().identifier()
				+ ": { type: "
				+ UnregisteredObjectException.getID(mapEntry.getValue().type())
				+ ", generator: "
				+ mapEntry.getValue().generator()
				+ " }"
			)
			.collect(Collectors.joining(", ", "{ ", " }"))
		);
	}
}