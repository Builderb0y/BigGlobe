package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.chunkgen.perSection.PaletteIdReplacer;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.scripted2.ColumnScript.ColumnYToDoubleScript.Catcher;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OreFeature extends AbstractOreFeature<OreFeature.Config> {

	public OreFeature(Codec<OreFeature.Config> codec) {
		super(codec);
	}

	public OreFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(OreFeature.Config.class));
	}

	@Override
	public OreBlockReplacer getReplacer(SectionGenerationContext context, OreFeature.Config config) {
		PaletteIdReplacer idReplacer = PaletteIdReplacer.of(context, config.blocks);
		return idReplacer != null ? new SingleOreBlockReplacer(idReplacer) : null;
	}

	public static class SingleOreBlockReplacer extends OreBlockReplacer {

		public final PaletteIdReplacer idReplacer;

		public SingleOreBlockReplacer(PaletteIdReplacer idReplacer) {
			this.idReplacer = idReplacer;
		}

		@Override
		public void replace(
			ScriptedColumn column,
			SectionGenerationContext context,
			int index,
			int blockX,
			int blockY,
			int blockZ,
			long blockSeed,
			double veinCenterX,
			double veinCenterY,
			double veinCenterZ,
			double veinRadius,
			double blockVeinFractionSquared
		) {
			double blockRNG = Permuter.toPositiveDouble(blockSeed);
			double blockChance = BigGlobeMath.squareD(1.0D - blockVeinFractionSquared);
			if (blockRNG < blockChance) {
				int oldID = context.storage().get(index);
				int newID = this.idReplacer.getReplacement(oldID);
				if (oldID != newID) context.storage().set(index, newID);
			}
		}
	}

	public static class Config extends AbstractOreFeature.Config {

		public final BlockState2ObjectMap<@VerifyNormal BlockState> blocks;

		public Config(
			Seed seed,
			Catcher chance,
			Catcher core_chance,
			RandomSource radius,
			BlockState2ObjectMap<@VerifyNormal BlockState> blocks
		) {
			super(seed, chance, core_chance, radius);
			this.blocks = blocks;
		}
	}
}