package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(WorldPresets.class)
public class WorldPresets_MakeBigGlobeTheDefaultWorldType2 {

	@WrapOperation(method = "createNormalWorldDimensions", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderLookup$RegistryLookup;getOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;"))
	private static Reference<WorldPreset> bigglobe_modifyDefaultPreset(HolderLookup.RegistryLookup<WorldPreset> instance, ResourceKey<WorldPreset> key, Operation<Reference<WorldPreset>> original) {
		Identifier identifier = Identifier.tryParse(BigGlobeConfig.INSTANCE.get().defaultWorldType);
		if (identifier != null) {
			ResourceKey<WorldPreset> newKey = ResourceKey.create(Registries.WORLD_PRESET, identifier);
			Reference<WorldPreset> result = instance.get(newKey).orElse(null);
			if (result != null) {
				return result;
			}
		}
		return original.call(instance, key);
	}
}