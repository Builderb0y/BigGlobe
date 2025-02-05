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

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.WorldPreset;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@Mixin(targets = "net.minecraft.server.dedicated.ServerPropertiesHandler$WorldGenProperties")
public class WorldGenProperties_LogLevelType {

	@Shadow @Final private String levelType;

	@Inject(method = "createDimensionsRegistryHolder", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/entry/RegistryEntry;value()Ljava/lang/Object;"))
	private void bigglobe_logLevelType(
		#if MC_VERSION >= MC_1_21_3
			WrapperLookup registries,
		#else
			DynamicRegistryManager registries,
		#endif
		CallbackInfoReturnable<DimensionOptionsRegistryHolder> callback,
		@Local(index = 4) RegistryEntry<WorldPreset> preset
	) {
		BigGlobeMod.LOGGER.info(
			"The level-type is "
			+ this.levelType
			+ ". The world preset has the following dimensions: "
			+ ((WorldPreset_DimensionsAccess)(preset.value()))
			.bigglobe_getDimensions()
			.entrySet()
			.stream()
			.map((Map.Entry<RegistryKey<DimensionOptions>, DimensionOptions> mapEntry) ->
				mapEntry.getKey().getValue()
				+ ": { type: "
				+ UnregisteredObjectException.getID(mapEntry.getValue().dimensionTypeEntry())
				+ ", generator: "
				+ mapEntry.getValue().chunkGenerator()
				+ " }"
			)
			.collect(Collectors.joining(", ", "{ ", " }"))
		);
	}
}