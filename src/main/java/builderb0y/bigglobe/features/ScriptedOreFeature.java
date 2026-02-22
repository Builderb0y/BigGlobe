package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.SectionGenerationContext;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.NotY;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.wrappers.BlockStateWrapper;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class ScriptedOreFeature extends AbstractOreFeature<ScriptedOreFeature.Config> {

	public ScriptedOreFeature(Codec<Config> codec) {
		super(codec);
	}

	public ScriptedOreFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public OreBlockReplacer getReplacer(SectionGenerationContext context, Config config) {
		return new ScriptedOreBlockReplacer(config.replacer_script);
	}

	public static class ScriptedOreBlockReplacer extends OreBlockReplacer {

		public final OreBlockReplacerScript.Holder script;

		public ScriptedOreBlockReplacer(OreBlockReplacerScript.Holder script) {
			this.script = script;
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
			BlockState oldState = context.palette().get(context.storage().get(index));
			BlockState newState;
			synchronized (column) {
				newState = this.script.getReplacement(
					column,
					oldState,
					blockX,
					blockY,
					blockZ,
					blockSeed,
					veinCenterX,
					veinCenterY,
					veinCenterZ,
					veinRadius,
					Math.sqrt(blockVeinFractionSquared)
				);
			}
			if (newState != null && newState != oldState) {
				int id = context.palette().index(newState #if MC_VERSION >= MC_1_21_9 , context.container() #endif);
				context.storage().set(index, id);
			}
		}
	}

	public static interface OreBlockReplacerScript extends ColumnScript {

		public abstract BlockState getReplacement(
			ScriptedColumn column,
			BlockState oldState,
			@NotY int blockX,
			int blockY,
			@NotY int blockZ,
			long blockSeed,
			double centerX,
			double centerY,
			double centerZ,
			double radius,
			double radialFraction
		);

		@Wrapper
		public static class Holder extends ColumnScript.BaseHolder<OreBlockReplacerScript> implements OreBlockReplacerScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void addExtraFunctionsToEnvironment(ImplParameters parameters, MutableScriptEnvironment environment) {
				super.addExtraFunctionsToEnvironment(parameters, environment);
				environment
				.addVariableLoad("oldState", BlockStateWrapper.TYPE)
				.addVariableLoad("blockX", TypeInfos.INT)
				//.addVariableLoad("blockY", TypeInfos.INT) //defined automatically by super method.
				.addVariableLoad("blockZ", TypeInfos.INT)
				.addVariableLoad("blockSeed", TypeInfos.LONG)
				.addVariableLoad("centerX", TypeInfos.DOUBLE)
				.addVariableLoad("centerY", TypeInfos.DOUBLE)
				.addVariableLoad("centerZ", TypeInfos.DOUBLE)
				.addVariableLoad("radius", TypeInfos.DOUBLE)
				.addVariableLoad("radialFraction", TypeInfos.DOUBLE)
				;
			}

			@Override
			public Class<OreBlockReplacerScript> getScriptClass() {
				return OreBlockReplacerScript.class;
			}

			@Override
			public BlockState getReplacement(
				ScriptedColumn column,
				BlockState oldState,
				int blockX,
				int blockY,
				int blockZ,
				long blockSeed,
				double centerX,
				double centerY,
				double centerZ,
				double radius,
				double radialFraction
			) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.getReplacement(
						column,
						oldState,
						blockX,
						blockY,
						blockZ,
						blockSeed,
						centerX,
						centerY,
						centerZ,
						radius,
						radialFraction
					);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return null;
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static class Config extends AbstractOreFeature.Config {

		public final OreBlockReplacerScript.Holder replacer_script;

		public Config(
			Seed seed,
			ColumnYToDoubleScript.Holder chance,
			ColumnYToDoubleScript.Holder core_chance,
			RandomSource radius,
			OreBlockReplacerScript.Holder replacer_script
		) {
			super(seed, chance, core_chance, radius);
			this.replacer_script = replacer_script;
		}
	}
}