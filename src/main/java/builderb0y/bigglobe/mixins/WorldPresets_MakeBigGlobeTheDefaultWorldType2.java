package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;

import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(WorldPresets.class)
public class WorldPresets_MakeBigGlobeTheDefaultWorldType2 {

	#if MC_VERSION > MC_1_21_1

		@WrapOperation(method = "createDemoOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryWrapper$Impl;getOrThrow(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;"))
		private static Reference<WorldPreset> bigglobe_modifyDefaultPreset(RegistryWrapper.Impl<WorldPreset> instance, RegistryKey<WorldPreset> key, Operation<Reference<WorldPreset>> original) {
			Identifier identifier = Identifier.tryParse(BigGlobeConfig.INSTANCE.get().defaultWorldType);
			if (identifier != null) {
				RegistryKey<WorldPreset> newKey = RegistryKey.of(RegistryKeys.WORLD_PRESET, identifier);
				Reference<WorldPreset> result = instance.getOptional(newKey).orElse(null);
				if (result != null) {
					return result;
				}
			}
			return original.call(instance, key);
		}

	#else

		@WrapOperation(method = "createDemoOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/Registry;entryOf(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/registry/entry/RegistryEntry$Reference;"))
		private static Reference<WorldPreset> bigglobe_modifyDefaultPreset(Registry<WorldPreset> instance, RegistryKey<WorldPreset> key, Operation<Reference<WorldPreset>> original) {
			Identifier identifier = Identifier.tryParse(BigGlobeConfig.INSTANCE.get().defaultWorldType);
			if (identifier != null) {
				RegistryKey<WorldPreset> newKey = RegistryKey.of(RegistryKeys.WORLD_PRESET, identifier);
				Reference<WorldPreset> result = instance.getEntry(newKey).orElse(null);
				if (result != null) {
					return result;
				}
			}
			return original.call(instance, key);
		}

	#endif
}