package builderb0y.bigglobe.features;

import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.DefaultString;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedFeature extends Feature<ScriptedFeature.Config> implements RawFeature<ScriptedFeature.Config> {

	public ScriptedFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ScriptedFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		if (context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			BlockPos origin = context.origin();
			Permuter permuter = Permuter.from(context.random());
			Symmetry symmetry;
			if (context.config().rotate_randomly) {
				if (context.config().flip_randomly) {
					symmetry = Symmetry.VALUES[permuter.nextInt(8)];
				}
				else {
					symmetry = Symmetry.VALUES[permuter.nextInt(4)];
				}
			}
			else {
				if (context.config().flip_randomly) {
					symmetry = Symmetry.VALUES[permuter.nextInt(4, 8)];
				}
				else {
					symmetry = Symmetry.IDENTITY;
				}
			}
			int chunkX = origin.getX() >> 4;
			int chunkZ = origin.getZ() >> 4;
			BoundingBox box;
			if (context.config().queueType == QueueType.DELAYED) {
				box = new BoundingBox(
					origin.getX() - 128,
					origin.getY() - 128,
					origin.getZ() - 128,
					origin.getX() + 127,
					origin.getY() + 127,
					origin.getZ() + 127
				);
			}
			else if (context.level() instanceof Level) {
				box = new BoundingBox(
					origin.getX() - 128,
					HeightLimitViewVersions.getMinY(context.level()),
					origin.getZ() - 128,
					origin.getX() + 127,
					HeightLimitViewVersions.getMaxY(context.level()),
					origin.getZ() + 127
				);
			}
			else {
				box = new BoundingBox(
					(chunkX - 1) << 4,
					HeightLimitViewVersions.getMinY(context.level()),
					(chunkZ - 1) << 4,
					((chunkX + 1) << 4) | 15,
					HeightLimitViewVersions.getMaxY(context.level()),
					((chunkZ + 1) << 4) | 15
				);
			}
			Coordination coordination = new Coordination(
				SymmetricOffset.fromCenter(origin.getX(), origin.getZ(), symmetry),
				box,
				box
			);
			WorldGenLevel world = switch (context.config().queueType) {
				case NONE -> context.level();
				case BASIC -> new BlockQueueStructureWorldAccess(
					context.level(),
					new BlockQueue(Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)
				);
				case DELAYED -> (
					new BlockQueueStructureWorldAccess(
						context.level(),
						new SerializableBlockQueue(
							origin.getX(),
							origin.getY(),
							origin.getZ(),
							Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
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
			WorldWrapper wrapper = new WorldWrapper(
				new WorldDelegator(world),
				generator,
				permuter,
				coordination,
				ColumnUsage.FEATURES.maybeDhHints()
			);
			wrapper.featureSalt = permuter.nextLong();
			if (
				context.config().script.generate(
					wrapper,
					origin.getX(),
					origin.getY(),
					origin.getZ()
				)
			) {
				if (context.config().queueType != QueueType.NONE) {
					((BlockQueueStructureWorldAccess)(world)).queue.placeQueuedBlocks(context.level());
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

	@Override
	public boolean generate(WorldWrapper world, Config config, BlockPos pos) {
		return config.script.generate(world, pos.getX(), pos.getY(), pos.getZ());
	}

	public static interface ScriptedFeatureImplementation extends Script {

		public abstract boolean generate(
			WorldWrapper world,
			int originX,
			int originY,
			int originZ
		)
			throws EarlyFeatureExitException;

		@Wrapper
		public static class Catcher extends ScriptCatcher<ScriptedFeatureImplementation> implements ScriptedFeatureImplementation {

			public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(ScriptedFeatureImplementation.class, this.usage, registry.parserFlags())
						.configureEnvironment(JavaUtilScriptEnvironment.withRandom(WORLD.random))
						.addEnvironment(MathScriptEnvironment.INSTANCE)
						.configureEnvironment(MinecraftScriptEnvironment.createWithWorld(WORLD.loadSelf))
						.configureEnvironment(WoodPaletteScriptEnvironment.create(WORLD.random))
						.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
						.configureEnvironment(NbtScriptEnvironment.createMutable())
						.configureEnvironment(RandomScriptEnvironment.create(WORLD.random))
						.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
						.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
						.configureEnvironment(GridScriptEnvironment.createWithSeed(WORLD.seed))
						.configureEnvironment((MutableScriptEnvironment environment) -> {
							registry.setupExternalEnvironment(
								environment
									.addVariableLoad("originX", TypeInfos.INT)
									.addVariableLoad("originY", TypeInfos.INT)
									.addVariableLoad("originZ", TypeInfos.INT)
									.addVariable("hints", WORLD.hints)
									.configure(ScriptedColumn.hintsEnvironment())
									.addVariable("distantHorizons", WORLD.distantHorizons)
									.addFunctionNoArgs("finish", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "FINISH"))))
									.addFunctionNoArgs("abort", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "ABORT")))),
								new ExternalEnvironmentParams()
									.withLookup(WORLD.loadSelf)
									.withXZ(
										load("originX", TypeInfos.INT),
										load("originZ", TypeInfos.INT)
									)
									.withY(load("originY", TypeInfos.INT))
							);
						})
						.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
						.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public boolean generate(WorldWrapper world, int originX, int originY, int originZ) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
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
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static class Config implements FeatureConfiguration {

		public final ScriptedFeatureImplementation.Catcher script;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean rotate_randomly;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean flip_randomly;
		public final @DefaultString("none")
		@UseName("queue") QueueType queueType;

		public Config(
			ScriptedFeatureImplementation.Catcher script,
			boolean rotate_randomly,
			boolean flip_randomly,
			QueueType queueType
		) {
			this.script          = script;
			this.rotate_randomly = rotate_randomly;
			this.flip_randomly   = flip_randomly;
			this.queueType       = queueType;
		}
	}

	public static enum QueueType implements StringRepresentable {
		NONE,
		BASIC,
		DELAYED;

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String getSerializedName() {
			return this.lowerCaseName;
		}
	}

	public static class EarlyFeatureExitException extends Exception {

		public static final EarlyFeatureExitException
			FINISH = new EarlyFeatureExitException(true),
			ABORT = new EarlyFeatureExitException(false);

		public final boolean placeBlocks;

		public EarlyFeatureExitException(boolean placeBlocks) {
			super(null, null, false, false);
			this.placeBlocks = placeBlocks;
		}
	}
}