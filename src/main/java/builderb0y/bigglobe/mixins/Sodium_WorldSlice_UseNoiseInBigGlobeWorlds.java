package builderb0y.bigglobe.mixins;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;

import builderb0y.bigglobe.ClientState;

/**
I change the way vanilla color providers work, see {@link BiomeColors_UseNoiseInBigGlobeWorlds}.
unfortunately, sodium overrides this logic, and doesn't call my modified code at all.
so, I need a 2nd mixin to handle sodium's code path too.
*/
@Mixin(WorldSlice.class)
public class Sodium_WorldSlice_UseNoiseInBigGlobeWorlds {

	@Inject(method = "getColor(Lme/jellysquid/mods/sodium/client/world/biome/BiomeColorSource;III)I", at = @At("HEAD"), cancellable = true, remap = false)
	private void bigglobe_useNoiseInBigGlobeWorlds(BiomeColorSource source, int x, int y, int z, CallbackInfoReturnable<Integer> callback) {
		ClientState.overrideColor(
			x, y, z,
			switch (source) {
				case GRASS   -> BiomeColors.  GRASS_COLOR;
				case FOLIAGE -> BiomeColors.FOLIAGE_COLOR;
				case WATER   -> BiomeColors.  WATER_COLOR;
			},
			callback
		);
	}
}