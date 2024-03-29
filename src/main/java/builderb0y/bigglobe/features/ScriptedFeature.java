package builderb0y.bigglobe.features;

import java.util.Locale;
import java.util.random.RandomGenerator;

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

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder.ColumnLookup;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder.DefaultLookupPosition;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.IdentityCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
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
		WorldWrapper wrapper = new WorldWrapper(new WorldDelegator(world), permuter, coordination);
		if (
			context.getConfig().script.generate(
				wrapper,
				origin.getX(),
				origin.getY(),
				origin.getZ(),
				DistantHorizonsCompat.isOnDistantHorizonThread()
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

	public static class Config implements FeatureConfig {

		public final FeatureScript.Holder script;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean rotate_randomly;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean flip_randomly;
		public final @DefaultString("none") @UseName("queue") QueueType queueType;

		public Config(FeatureScript.Holder script, boolean rotate_randomly, boolean flip_randomly, QueueType queueType) {
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

	public static interface FeatureScript extends Script {

		public abstract boolean generate(
			WorldWrapper world,
			int originX,
			int originY,
			int originZ,
			boolean distantHorizons
		)
		throws EarlyFeatureExitException;

		@Wrapper
		public static class Holder extends ScriptHolder<FeatureScript> implements FeatureScript {

			public static final InsnTree
				LOAD_WORLD = load("world", 1, WorldWrapper.TYPE),
				LOAD_RANDOM = new IdentityCastInsnTree(
					getField(
						LOAD_WORLD,
						FieldInfo.getField(WorldWrapper.class, "permuter")
					),
					type(RandomGenerator.class)
				);

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) throws ScriptParsingException {
				super(
					usage,
					new TemplateScriptParser<>(FeatureScript.class, usage)
					.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(MinecraftScriptEnvironment.createWithWorld(LOAD_WORLD))
					.addEnvironment(CoordinatorScriptEnvironment.create(LOAD_WORLD))
					.addEnvironment(NbtScriptEnvironment.INSTANCE)
					.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
					.addEnvironment(
						new MutableScriptEnvironment()
						.addVariableLoad("originX", 2, TypeInfos.INT)
						.addVariableLoad("originY", 3, TypeInfos.INT)
						.addVariableLoad("originZ", 4, TypeInfos.INT)
						.addVariableLoad("distantHorizons", 5, TypeInfos.BOOLEAN)
						.addFunctionNoArgs("finish", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "FINISH"))))
						.addFunctionNoArgs("abort",  throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "ABORT" ))))
					)
					.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.addEnvironment(
						ColumnScriptEnvironmentBuilder.createFromLookup(
							ColumnValue.REGISTRY,
							new IdentityCastInsnTree(
								LOAD_WORLD,
								type(ColumnLookup.class)
							),
							new DefaultLookupPosition(
								load("originX", 2, TypeInfos.INT),
								new OpcodeCastInsnTree(
									load("originY", 3, TypeInfos.INT),
									I2D,
									TypeInfos.DOUBLE
								),
								load("originZ", 4, TypeInfos.INT)
							)
						)
						.build()
					)
					.addEnvironment(StructureTemplateScriptEnvironment.create(LOAD_WORLD))
					.parse()
				);
			}

			@Override
			public boolean generate(WorldWrapper world, int originX, int originY, int originZ, boolean distantHorizons) {
				try {
					return this.script.generate(world, originX, originY, originZ, distantHorizons);
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