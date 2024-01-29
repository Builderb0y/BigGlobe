package builderb0y.bigglobe.features;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultString;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
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
		if (context.getGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			BlockPos origin = context.getOrigin();
			ScriptedColumn column = generator.columnEntryRegistry.columnFactory.create(
				generator.seed,
				origin.getX(),
				origin.getZ(),
				context.getWorld().getBottomY(),
				context.getWorld().getTopY(),
				DistantHorizonsCompat.isOnDistantHorizonThread()
			);
			Permuter permuter = Permuter.from(context.getRandom());
			Symmetry symmetry;
			if (context.getConfig().rotate_randomly) {
				if (context.getConfig().flip_randomly) {
					symmetry = Symmetry.VALUES[permuter.nextInt(8)];
				}
				else {
					symmetry = Symmetry.VALUES[permuter.nextInt(4)];
				}
			}
			else {
				if (context.getConfig().flip_randomly) {
					symmetry = Symmetry.VALUES[permuter.nextInt(4, 8)];
				}
				else {
					symmetry = Symmetry.IDENTITY;
				}
			}
			int chunkX = origin.getX() >> 4;
			int chunkZ = origin.getZ() >> 4;
			BlockBox box = (
				context.getConfig().queueType == QueueType.DELAYED
				? new BlockBox(
					origin.getX() - 128,
					origin.getY() - 128,
					origin.getZ() - 128,
					origin.getX() + 127,
					origin.getY() + 127,
					origin.getZ() + 127
				)
				: new BlockBox(
					(chunkX - 1) << 4,
					context.getWorld().getBottomY(),
					(chunkZ - 1) << 4,
					((chunkX + 1) << 4) | 15,
					context.getWorld().getTopY(),
					((chunkZ + 1) << 4) | 15
				)
			);
			Coordination coordination = new Coordination(
				SymmetricOffset.fromCenter(origin.getX(), origin.getZ(), symmetry),
				box,
				box
			);
			StructureWorldAccess world = switch (context.getConfig().queueType) {
				case NONE -> context.getWorld();
				case BASIC -> new BlockQueueStructureWorldAccess(
					context.getWorld(),
					new BlockQueue(Block.NOTIFY_LISTENERS | Block.FORCE_STATE)
				);
				case DELAYED -> (
					new BlockQueueStructureWorldAccess(
						context.getWorld(),
						new SerializableBlockQueue(
							origin.getX(),
							origin.getY(),
							origin.getZ(),
							Block.NOTIFY_LISTENERS | Block.FORCE_STATE
						)
					) {

						@Override
						public void setBlockState(BlockPos pos, BlockState state) {
							BlockState oldState = this.getWorldState(pos);
							if (SerializableBlockQueue.canImplicitlyReplace(oldState)) {
								this.queue.queueBlock(pos, state);
							}
							else {
								this.queue.queueReplacement(pos, oldState, state);
							}
						}
					}
				);
			};
			WorldWrapper wrapper = new WorldWrapper(new WorldDelegator(world), permuter, coordination, column);
			if (
				context.getConfig().script.generate(
					wrapper,
					origin.getX(),
					origin.getY(),
					origin.getZ()
				)
			) {
				if (context.getConfig().queueType != QueueType.NONE) {
					((BlockQueueStructureWorldAccess)(world)).queue.placeQueuedBlocks(context.getWorld());
				}
				return true;
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	public static interface ScriptedFeatureImplementation extends Script {

		public abstract boolean generate(
			WorldWrapper world,
			int originX,
			int originY,
			int originZ
		)
		throws EarlyFeatureExitException;

		public static class Holder extends ScriptHolder<ScriptedFeatureImplementation> implements ScriptedFeatureImplementation {

			public static final InsnTree
				LOAD_WORLD = load("world", type(WorldWrapper.class)),
				LOAD_RANDOM = getField(LOAD_WORLD, FieldInfo.getField(WorldWrapper.class, "random")),
				LOAD_COLUMN = getField(LOAD_WORLD, FieldInfo.getField(WorldWrapper.class, "column")),
				DISTANT_HORIZONS = getField(LOAD_COLUMN, FieldInfo.getField(ScriptedColumn.class, "distantHorizons"));

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(ScriptedFeatureImplementation.class, this.usage)
					.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(MinecraftScriptEnvironment.createWithWorld(LOAD_WORLD))
					.addEnvironment(CoordinatorScriptEnvironment.create(LOAD_WORLD))
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.addEnvironment(StructureTemplateScriptEnvironment.create(LOAD_WORLD))
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						registry.setupExternalEnvironment(
							environment
							.addVariableLoad("originX", TypeInfos.INT)
							.addVariableLoad("originY", TypeInfos.INT)
							.addVariableLoad("originZ", TypeInfos.INT)
							.addVariable("distantHorizons", DISTANT_HORIZONS)
							.addFunctionNoArgs("finish", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "FINISH"))))
							.addFunctionNoArgs("abort",  throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "ABORT" )))),
							new DirectCastInsnTree(LOAD_COLUMN, registry.columnContext.columnType())
						);
					})
					.parse()
				);
			}

			@Override
			public boolean generate(WorldWrapper world, int originX, int originY, int originZ) {
				try {
					return this.script.generate(world, originX, originY, originZ);
				}
				catch (EarlyFeatureExitException exit) {
					return exit.placeBlocks;
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static class Config implements FeatureConfig {

		public final ScriptedFeatureImplementation.Holder script;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean rotate_randomly;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean flip_randomly;
		public final @DefaultString("none") @UseName("queue") QueueType queueType;

		public Config(
			ScriptedFeatureImplementation.Holder script,
			boolean rotate_randomly,
			boolean flip_randomly,
			QueueType queueType
		) {
			this.script = script;
			this.rotate_randomly = rotate_randomly;
			this.flip_randomly = flip_randomly;
			this.queueType = queueType;
		}
	}

	public static enum QueueType implements StringIdentifiable {
		NONE,
		BASIC,
		DELAYED;

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String asString() {
			return this.lowerCaseName;
		}
	}

	public static class EarlyFeatureExitException extends Exception {

		public static final EarlyFeatureExitException
			FINISH = new EarlyFeatureExitException(true),
			ABORT  = new EarlyFeatureExitException(false);

		public final boolean placeBlocks;

		public EarlyFeatureExitException(boolean placeBlocks) {
			super(null, null, false, false);
			this.placeBlocks = placeBlocks;
		}
	}
}