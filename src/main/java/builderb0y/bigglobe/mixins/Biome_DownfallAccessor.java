package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Weather;

import builderb0y.bigglobe.scripting.wrappers.BiomeEntry.BiomeDownfallAccessor;

@Mixin(Biome.class)
public class Biome_DownfallAccessor implements BiomeDownfallAccessor {

	@Shadow @Final private Weather weather;

	@Override
	public float bigglobe_getDownfall() {
		return this.weather.downfall();
	}
}