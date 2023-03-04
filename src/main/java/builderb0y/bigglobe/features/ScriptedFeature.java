package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import org.objectweb.asm.Opcodes;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.FeatureColumns;
import builderb0y.bigglobe.chunkgen.FeatureColumns.ColumnSupplier;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.Wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.CastingSupport;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment2;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedFeature extends Feature<ScriptedFeature.Config> {

	public ScriptedFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ScriptedFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		return context.getConfig().generate(context);
	}

	public static record Config(FeatureScript.Holder script) implements FeatureConfig {

		public boolean generate(FeatureContext<Config> context) {
			BlockPos origin = context.getOrigin();
			ColumnSupplier oldSupplier = FeatureColumns.FEATURE_COLUMNS.get();
			WorldColumn column = FeatureColumns.get(context.getWorld(), origin.getX(), origin.getZ(), oldSupplier);
			try {
				FeatureColumns.FEATURE_COLUMNS.set(ColumnSupplier.fixedPosition(column));
				return this.script.generate(
					new WorldWrapper(context.getWorld(), Permuter.from(context.getRandom())),
					origin.getX(),
					origin.getY(),
					origin.getZ(),
					column
				);
			}
			finally {
				FeatureColumns.FEATURE_COLUMNS.set(oldSupplier);
			}
		}
	}

	public static interface FeatureScript extends Script {

		public abstract boolean generate(
			WorldWrapper world,
			int originX,
			int originY,
			int originZ,
			WorldColumn column
		);

		@Wrapper
		public static class Holder extends ScriptHolder<FeatureScript> implements FeatureScript {

			public Holder(String script) throws ScriptParsingException {
				super(
					new ScriptParser<>(FeatureScript.class, script)
					.addEnvironment(JavaUtilScriptEnvironment.ALL)
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addCastProvider(MinecraftScriptEnvironment2.CAST_PROVIDER)
					.addEnvironment(new MinecraftScriptEnvironment2(
						load("world", 1, WorldWrapper.TYPE)
					))
					.addEnvironment(NBTScriptEnvironment.INSTANCE)
					.addCastProvider(NBTScriptEnvironment.NBT_CASTS)
					.addEnvironment(
						new MutableScriptEnvironment2()
						.addVariableLoad("originX", 2, TypeInfos.INT)
						.addVariableLoad("originY", 3, TypeInfos.INT)
						.addVariableLoad("originZ", 4, TypeInfos.INT)
					)
					.addEnvironment(new RandomScriptEnvironment2(
						getField(
							load("world", 1, WorldWrapper.TYPE),
							field(
								Opcodes.ACC_PUBLIC,
								WorldWrapper.TYPE,
								"permuter",
								type(Permuter.class)
							)
						)
					))
					.addEnvironment(new ColumnYScriptEnvironment(
						load("column", 5, ColumnYScriptEnvironment.WORLD_COLUMN_TYPE),
						CastingSupport.primitiveCast(load("originY", 3, TypeInfos.INT), TypeInfos.DOUBLE, CastMode.EXPLICIT_THROW),
						false
					))
					.parse()
				);
			}

			@Override
			public boolean generate(WorldWrapper world, int originX, int originY, int originZ, WorldColumn column) {
				try {
					return this.script.generate(world, originX, originY, originZ, column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}
}