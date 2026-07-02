package builderb0y.bigglobe.features;

import java.util.Locale;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;
import builderb0y.bigglobe.blockdefs.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.ReadOnlyWorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.Tripwire;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedFeature extends Feature<ScriptedFeature.Config> implements RawFeature<ScriptedFeature.Config> {

	public ScriptedFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ScriptedFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	public Symmetry getSymmetry(Config config, RandomGenerator random) {
		if (config.rotate_randomly) {
			if (config.flip_randomly) {
				return Symmetry.VALUES[random.nextInt(8)];
			}
			else {
				return Symmetry.VALUES[random.nextInt(4)];
			}
		}
		else {
			if (config.flip_randomly) {
				return Symmetry.VALUES[random.nextInt(4, 8)];
			}
			else {
				return Symmetry.IDENTITY;
			}
		}
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		if (context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			WorldGenLevel originalWorld = context.level();
			if (context.config().queueType == QueueType.DELAYED && !DistantHorizonsCompat.isOnDistantHorizonThread() && !(originalWorld instanceof ServerLevel)) {
				return delay(context);
			}
			else {
				BlockPos origin = context.origin();
				Permuter permuter = Permuter.from(context.random());
				Symmetry symmetry = this.getSymmetry(context.config(), permuter);
				int radius = context.config().max_radius_in_blocks;
				BoundingBox mutableArea = new BoundingBox(
					origin.getX() - radius,
					HeightLimitViewVersions.getMinY(originalWorld),
					origin.getZ() - radius,
					origin.getX() + radius,
					HeightLimitViewVersions.getMaxY(originalWorld) - 1,
					origin.getZ() + radius
				);
				BoundingBox immutableArea = new BoundingBox(
					mutableArea.minX() & ~15,
					mutableArea.minY(),
					mutableArea.minZ() & ~15,
					mutableArea.maxX() | 15,
					mutableArea.maxY(),
					mutableArea.maxZ() | 15
				);
				Coordination coordination = new Coordination(
					new SymmetricOffset(origin.getX(), origin.getY(), origin.getZ(), symmetry),
					mutableArea,
					immutableArea
				);
				WorldGenLevel fakeWorld = (
					context.config().queueType != QueueType.NONE
					? new BlockQueueStructureWorldAccess(
						originalWorld,
						new BlockQueue(false)
					)
					: originalWorld
				);
				WorldWrapper wrapper = new WorldWrapper(
					new WorldDelegator(fakeWorld),
					generator,
					permuter,
					coordination,
					ColumnUsage.FEATURES.maybeDhHints()
				);
				wrapper.featureSalt = permuter.nextLong();
				if (context.config().script.generate(wrapper)) {
					if (context.config().queueType != QueueType.NONE) {
						((BlockQueueStructureWorldAccess)(fakeWorld)).queue.placeQueuedBlocks(originalWorld);
					}
					return true;
				}
				else {
					return false;
				}
			}
		}
		else {
			return false;
		}
	}

	public static boolean delay(FeaturePlaceContext<? extends SizedDelayedFeatureConfig> context) {
		WorldGenLevel originalWorld = context.level();
		BlockPos origin = context.origin();
		ConfiguredFeature<?, ?> feature = context.topFeature().orElse(null);
		if (feature != null) {
			Identifier id = RegistryVersions.getRegistry(originalWorld.registryAccess(), Registries.CONFIGURED_FEATURE).getKey(feature);
			if (id != null) {
				BlockState oldState = originalWorld.getBlockState(origin);
				BlockEntity oldBlockEntity = originalWorld.getBlockEntity(origin);
				CompoundTag oldBlockData = oldBlockEntity == null ? null : BlockEntityVersions.writeToNbt(oldBlockEntity);
				WorldUtil.setBlockState(originalWorld, origin, BlockStates.DELAYED_GENERATION, Block.UPDATE_CLIENTS);
				DelayedGenerationBlockEntity blockEntity = WorldUtil.getBlockEntity(originalWorld, origin, DelayedGenerationBlockEntity.class);
				if (blockEntity != null) {
					blockEntity.feature = id;
					blockEntity.oldState = oldState;
					blockEntity.oldBlockData = oldBlockData;
				}
				return true;
			}
			else if (Tripwire.isEnabled()) {
				Tripwire.logWithStackTrace("Attempt to place unregistered ScriptedFeature");
			}
		}
		else if (Tripwire.isEnabled()) {
			Tripwire.logWithStackTrace("Attempt to place ScriptedFeature from a FeaturePlaceContext which lacks a ConfiguredFeature.");
		}
		return false;
	}

	@Override
	public boolean generate(WorldWrapper world, Config config, BlockPos pos) {
		return config.script.generate(
			new WorldWrapper(
				world,
				new Coordination(
					new SymmetricOffset(
						pos.getX(),
						pos.getY(),
						pos.getZ(),
						world.coordination.transformation().symmetry().andThen(this.getSymmetry(config, world.random))
					),
					world.coordination.mutableArea(),
					world.coordination.immutableArea()
				)
			)
		);
	}

	public static interface ScriptedFeatureImplementation extends Script {

		public abstract boolean generate(WorldWrapper world) throws EarlyFeatureExitException;

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
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.configureEnvironment(CoordinatorScriptEnvironment.create(WORLD.loadSelf))
					.configureEnvironment(NbtScriptEnvironment.createMutable())
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment(StructureTemplateScriptEnvironment.create(WORLD.loadSelf))
					.configureEnvironment(GridScriptEnvironment.createWithSeed(ReadOnlyWorldWrapper.INFO.seed(WORLD.loadSelf)))
					.configure((ExpressionParser parser) -> {
						parser
						.environment
						.mutable()
						.addVariableConstant("originX", 0)
						.addVariableConstant("originY", 0)
						.addVariableConstant("originZ", 0)
						.addVariable("placementX", WORLD.originX)
						.addVariable("placementY", WORLD.originY)
						.addVariable("placementZ", WORLD.originZ)
						.addFunctionNoArgs("finish", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "FINISH"))))
						.addFunctionNoArgs("abort", throw_(getStatic(FieldInfo.getField(EarlyFeatureExitException.class, "ABORT"))))
						;
						registry.setupEnvironment(
							parser,
							new ExternalEnvironmentParams()
							.withLookup("world", WORLD.loadSelf)
							//world handles translations.
							.withXZ(ldc(0), ldc(0))
							.withY(ldc(0))
							.offsetY(WORLD.originY)
						);
					})
					.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
					.addImportedValue("random", ReadOnlyWorldWrapper.INFO.random(WORLD.loadSelf))
					.parse(new ScriptClassLoader(registry.loader))
				);
			}

			@Override
			public boolean generate(WorldWrapper world) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					return this.script.generate(world);
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

	@UseVerifier(name = "verify", in = Config.class, usage = MemberUsage.METHOD_IS_HANDLER)
	public static class Config implements FeatureConfiguration, SizedDelayedFeatureConfig {

		public final ScriptedFeatureImplementation.Catcher script;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean rotate_randomly;
		public final @DefaultBoolean(value = false, alwaysEncode = true) boolean flip_randomly;
		public final @DefaultString("none") @UseName("queue") QueueType queueType;
		public final @DefaultInt(16) int max_radius_in_blocks;

		public Config(
			ScriptedFeatureImplementation.Catcher script,
			boolean rotate_randomly,
			boolean flip_randomly,
			QueueType queueType,
			int max_radius_in_blocks
		) {
			this.script               = script;
			this.rotate_randomly      = rotate_randomly;
			this.flip_randomly        = flip_randomly;
			this.queueType            = queueType;
			this.max_radius_in_blocks = max_radius_in_blocks;
		}

		public static <T_Encoded> void verify(VerifyContext<T_Encoded, Config> context) throws VerifyException {
			Config config = context.object;
			if (config == null) return;
			if (config.max_radius_in_blocks > 16 && config.queueType != QueueType.DELAYED) {
				throw new VerifyException(() -> "queue must be 'delayed' when max_radius_in_blocks is greater than 16.");
			}
		}

		@Override
		public int getMaxRadiusInBlocks() {
			return this.max_radius_in_blocks;
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
			ABORT  = new EarlyFeatureExitException(false);

		public final boolean placeBlocks;

		public EarlyFeatureExitException(boolean placeBlocks) {
			super(null, null, false, false);
			this.placeBlocks = placeBlocks;
		}
	}
}