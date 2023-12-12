package builderb0y.bigglobe.features.ores;

import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class NetherOreFeature extends OreFeature<NetherOreFeature.Config> {

	public NetherOreFeature() {
		super(Config.class);
	}

	@AddPseudoField("place")
	@AddPseudoField("replace")
	public static class Config extends OreFeature.Config {

		public final TagKey<Biome> biomes;

		public Config(
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius,
			BlockState2ObjectMap<BlockState> blocks,
			@VerifyNormal @VerifyNullable BlockState place,
			@VerifyNormal @VerifyNullable BlockState replace,
			TagKey<Biome> biomes
		) {
			super(chance, radius, blocks);
			if (place != null && replace != null) {
				this.blocks.serializedStates.put(BlockArgumentParser.stringifyBlockState(replace), place);
				this.blocks.runtimeStates.put(replace, place);
			}
			this.biomes = biomes;
		}

		@Override
		public boolean canSpawnAt(WorldColumn column, int y) {
			return column.getBiome(y).isIn(this.biomes);
		}

		//backwards compatibility.

		public @VerifyNormal @VerifyNullable BlockState place() {
			return null;
		}

		public @VerifyNormal @VerifyNullable BlockState replace() {
			return null;
		}
	}
}