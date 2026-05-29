package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.ClimateSettings;

import builderb0y.bigglobe.mixinInterfaces.BiomeDownfallAccessor;

@Mixin(Biome.class)
public class Biome_DownfallAccessor implements BiomeDownfallAccessor {

	@Shadow
	@Final
	private ClimateSettings climateSettings;

	@Override
	public float bigglobe_getDownfall() {
		return this.climateSettings.downfall();
	}
}