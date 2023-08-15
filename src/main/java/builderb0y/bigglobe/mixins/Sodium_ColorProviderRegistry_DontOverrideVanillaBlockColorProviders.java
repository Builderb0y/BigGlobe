package builderb0y.bigglobe.mixins;

import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
I change the way vanilla color providers work, see {@link BiomeColors_UseNoiseInBigGlobeWorlds}.
unfortunately, sodium overrides this logic for unknown reasons, and doesn't call my modified code at all.
so, I am reverting this "feature" so that my colors work as expected.
*/
@Mixin(ColorProviderRegistry.class)
public class Sodium_ColorProviderRegistry_DontOverrideVanillaBlockColorProviders {

	/**
	@author Builderb0y
	@reason sodium doesn't call my logic correctly.
	*/
	@Overwrite
	private void installOverrides() {}
}