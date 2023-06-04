package builderb0y.bigglobe.settings;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.level.ColorResolver;

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

	public int getGrassColor(BlockPos pos) {
		double temperature = this.getTemperature(pos.getX(), pos.getY(), pos.getZ());
		double foliage = this.getFoliage(pos.getX(), pos.getY(), pos.getZ());
		return GrassColors.getColor(temperature, foliage);
	}

	public int getFoliageColor(BlockPos pos) {
		double temperature = this.getTemperature(pos.getX(), pos.getY(), pos.getZ());
		double foliage = this.getFoliage(pos.getX(), pos.getY(), pos.getZ());
		return FoliageColors.getColor(temperature, foliage);
	}

	public int getWaterColor(BlockPos pos) {
		double temperature = this.getTemperature(pos.getX(), pos.getY(), pos.getZ());
		return MathHelper.packRgb(0.25F, (float)(temperature * 0.5D + 0.25D), 1.0F);
	}

	public static void overrideColor(BlockPos pos, ColorResolver colorResolver, CallbackInfoReturnable<Integer> callback) {
		OverworldClientSettings settings = ClientState.settings;
		if (settings != null) {
			if (colorResolver == BiomeColors.GRASS_COLOR) {
				callback.setReturnValue(settings.getGrassColor(pos));
			}
			else if (colorResolver == BiomeColors.FOLIAGE_COLOR) {
				callback.setReturnValue(settings.getFoliageColor(pos));
			}
			else if (colorResolver == BiomeColors.WATER_COLOR) {
				callback.setReturnValue(settings.getWaterColor(pos));
			}
		}
	}
}