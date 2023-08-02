package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;

@Mixin(WorldPresets.class)
public class WorldPresets_MakeBigGlobeTheDefaultWorldType2 {

	@Redirect(method = "createDefaultOptions(Lnet/minecraft/registry/DynamicRegistryManager;JZZ)Lnet/minecraft/world/gen/GeneratorOptions;", at = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"))
	private static RegistryKey<WorldPreset> bigglobe_getDefaultWorldPreset() {
		return BigGlobeConfig.INSTANCE.get().makeBigGlobeDefaultWorldType ? BigGlobeMod.BIG_GLOBE_WORLD_PRESET_KEY : WorldPresets.DEFAULT;
	}
}