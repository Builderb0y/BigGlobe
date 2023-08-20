package builderb0y.bigglobe.settings;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.world.biome.ColorResolver;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.HeightAdjustmentScript;

public record OverworldClientSettings(
	long seed,
	ClientTemperatureSettings temperature,
	ClientFoliageSettings foliage,
	int sea_level
) {

	public static final AutoCoder<OverworldClientSettings> NULLABLE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(new ReifiedType<@VerifyNullable OverworldClientSettings>() {});

	public static record ClientTemperatureSettings(Grid2D noise, HeightAdjustmentScript.TemperatureHolder height_adjustment) {}

	public static record ClientFoliageSettings(Grid2D noise, HeightAdjustmentScript.FoliageHolder height_adjustment) {}

	public static OverworldClientSettings of(long worldSeed, OverworldSettings settings) {
		return new OverworldClientSettings(
			Permuter.stafford(worldSeed),
			new ClientTemperatureSettings(settings.temperature.noise(), settings.temperature.height_adjustment()),
			new ClientFoliageSettings(settings.foliage.noise(), settings.foliage.height_adjustment()),
			settings.height.sea_level()
		);
	}

	public double getTemperature(int x, int y, int z) {
		return BigGlobeMath.sigmoid01(
			this.temperature.height_adjustment.evaluate(
				this.temperature.noise.getValue(this.seed, x, z),
				this.sea_level,
				y
			)
		);
	}

	public double getFoliage(int x, int y, int z) {
		return BigGlobeMath.sigmoid01(
			this.foliage.height_adjustment.evaluate(
				this.foliage.noise.getValue(this.seed, x, z),
				this.sea_level,
				y
			)
		);
	}

	public int getGrassColor(int x, int y, int z) {
		double temperature = this.getTemperature(x, y, z);
		double foliage = this.getFoliage(x, y, z);
		return GrassColors.getColor(temperature, foliage);
	}

	public int getFoliageColor(int x, int y, int z) {
		double temperature = this.getTemperature(x, y, z);
		double foliage = this.getFoliage(x, y, z);
		return FoliageColors.getColor(temperature, foliage);
	}

	public int getWaterColor(int x, int y, int z) {
		double temperature = this.getTemperature(x, y, z);
		return 0xFF3F00FF | (((int)(temperature * 128.0D + 64.0D)) << 8);
	}

	public static void overrideColor(int x, int y, int z, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		OverworldClientSettings settings = ClientState.settings;
		if (settings != null) {
			if (colorResolver == BiomeColors.GRASS_COLOR) {
				callback.setReturnValue(settings.getGrassColor(x, y, z));
			}
			else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
				callback.setReturnValue(settings.getFoliageColor(x, y, z));
			}
			else if (colorResolver == BiomeColors.WATER_COLOR) {
				callback.setReturnValue(settings.getWaterColor(x, y, z));
			}
		}
	}
}