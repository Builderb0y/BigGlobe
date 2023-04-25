package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.FeatureColumns;
import builderb0y.bigglobe.chunkgen.FeatureColumns.ColumnSupplier;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptInputs.SerializableScriptInputs;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
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
		BlockPos origin = context.getOrigin();
		ColumnSupplier oldSupplier = FeatureColumns.FEATURE_COLUMNS.get();
		WorldColumn column = FeatureColumns.get(context.getWorld(), origin.getX(), origin.getZ(), oldSupplier);
		Permuter permuter = Permuter.from(context.getRandom());
		BlockRotation rotation = (
			context.getConfig().rotate_randomly
			? Permuter.choose(permuter, Directions.ROTATIONS)
			: BlockRotation.NONE
		);
		Coordinator coordinator = Coordinator.forWorld(context.getWorld(), Block.NOTIFY_ALL);
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		coordinator = coordinator.inBox(
			(chunkX - 1) << 4,
			context.getWorld().getBottomY(),
			(chunkZ - 1) << 4,
			((chunkX + 1) << 4) | 15,
			context.getWorld().getTopY(),
			((chunkZ + 1) << 4) | 15
		);
		if (rotation != BlockRotation.NONE) {
			coordinator = (
				coordinator
				.translate(origin.getX(), origin.getY(), origin.getZ())
				.rotate1x(rotation)
				.translate(-origin.getX(), -origin.getY(), -origin.getZ())
			);
		}
		try {
			FeatureColumns.FEATURE_COLUMNS.set(ColumnSupplier.fixedPosition(column));
			return context.getConfig().script.generate(
				new WorldWrapper(context.getWorld(), coordinator, permuter),
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

	public static class Config implements FeatureConfig {

		public final FeatureScript.@EncodeInline Holder script;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean rotate_randomly;

		public Config(FeatureScript.Holder script, boolean rotate_randomly) {
			this.script = script;
			this.rotate_randomly = rotate_randomly;
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

			public static final InsnTree LOAD_RANDOM = getField(
				load("world", 1, WorldWrapper.TYPE),
				FieldInfo.getField(WorldWrapper.class, "permuter")
			);

			public final SerializableScriptInputs inputs;

			public Holder(SerializableScriptInputs inputs) throws ScriptParsingException {
				super(
					new TemplateScriptParser<>(FeatureScript.class, inputs.buildScriptInputs())
					.addEnvironment(JavaUtilScriptEnvironment.ALL)
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(MinecraftScriptEnvironment.createWithWorld(
						load("world", 1, WorldWrapper.TYPE)
					))
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
					.addEnvironment(
						new MutableScriptEnvironment()
						.addVariableLoad("originX", 2, TypeInfos.INT)
						.addVariableLoad("originY", 3, TypeInfos.INT)
						.addVariableLoad("originZ", 4, TypeInfos.INT)
					)
					.addEnvironment(new RandomScriptEnvironment(LOAD_RANDOM))
					.addEnvironment(
						ColumnScriptEnvironment.createFixedXYZ(
							ColumnValue.REGISTRY,
							load("column", 5, type(WorldColumn.class)),
							new OpcodeCastInsnTree(
								load("originY", 3, TypeInfos.INT),
								I2D,
								TypeInfos.DOUBLE
							)
						)
						.mutable
					)
					.parse()
				);
				this.inputs = inputs;
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