package builderb0y.bigglobe.features;

import java.util.*;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnRandomYToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.branches.BranchesConfig;
import builderb0y.bigglobe.trees.branches.ScriptedBranchShape.Catcher;
import builderb0y.bigglobe.trees.decoration.BallLeafDecorator;
import builderb0y.bigglobe.trees.decoration.BlockDecorator;
import builderb0y.bigglobe.trees.decoration.DecoratorConfig;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;
import builderb0y.bigglobe.trees.trunks.TrunkFactory;
import builderb0y.bigglobe.util.BlockState2ObjectMap;
import builderb0y.bigglobe.util.Directions;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

public class ArtificialTreeFeature extends Feature<ArtificialTreeFeature.Config> {

	public ArtificialTreeFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ArtificialTreeFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		if (!(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return false;
		WorldGenLevel world = context.level();
		Config config = context.config();
		IRandomList<Holder<Block>> saplingBlocks = config.palette.value().saplingBlocks();
		BlockPos origin = context.origin();
		if (!saplingBlocks.contains(world.getBlockState(origin).typeHolder())) return false;
		Permuter permuter = Permuter.from(context.random());
		ScriptedColumn column = generator.newColumn(world, origin.getX(), origin.getZ(), ColumnUsage.GENERIC.normalHints());
		int maxSaplingCount = config.max_saplings != null ? config.max_saplings.get(column, permuter, origin.getY()) : 64;
		if (maxSaplingCount <= 0) return false;
		BlockQueue blockQueue = new BlockQueue(true);
		Deque<BlockPos> toCheck = new ArrayDeque<>(8);
		blockQueue.queueBlock(origin, BlockStates.AIR);
		toCheck.add(origin);
		double centerX = origin.getX();
		int centerY = origin.getY();
		double centerZ = origin.getZ();
		outer:
		for (BlockPos pos; (pos = toCheck.pollFirst()) != null; ) {
			for (Direction direction : Directions.HORIZONTAL) {
				BlockPos offset = pos.relative(direction);
				if (blockQueue.getBlockStateOrNull(offset) == null && saplingBlocks.contains(world.getBlockState(offset).typeHolder())) {
					blockQueue.queueBlock(offset, BlockStates.AIR);
					centerX += offset.getX();
					centerZ += offset.getZ();
					if (blockQueue.blockCount() >= maxSaplingCount) break outer;
					toCheck.addLast(offset);
				}
			}
		}
		double saplingCount = blockQueue.blockCount();
		centerX = centerX / saplingCount + 0.5D;
		centerZ = centerZ / saplingCount + 0.5D;
		centerX += Permuter.nextUniformDouble(permuter) * 0.5D;
		centerZ += Permuter.nextUniformDouble(permuter) * 0.5D;
		double baseRadius = Math.sqrt(saplingCount / Math.PI);
		column = generator.newColumn(world, BigGlobeMath.floorI(centerX), BigGlobeMath.floorI(centerZ), ColumnUsage.GENERIC.normalHints());
		int trunkHeight = config.height.getHeight(column, baseRadius, permuter);
		if (trunkHeight <= 0) return false;
		TrunkConfig trunkConfig = config.trunk.create(
			column,
			centerX,
			centerY,
			centerZ,
			trunkHeight,
			permuter
		);
		double startFracY = config.branches.start_frac_y.get(column, centerY, permuter);
		BranchesConfig branchesConfig = BranchesConfig.create(
			startFracY,
			Permuter.roundRandomlyI(permuter, config.branches.count_per_layer.get(column, centerY, permuter) * trunkHeight * (1.0D - startFracY)),
			permuter.nextDouble(BigGlobeMath.TAU),
			trunkConfig.baseRadius,
			config.branches.length_function,
			config.branches.height_function
		);
		DecoratorConfig.Builder decorationsBuilder = new DecoratorConfig.Builder();
		if (config.decorations != null) config.decorations.addTo(decorationsBuilder);

		ScriptedColumnLookup.Impl columns = generator.newColumnLookup(world, ColumnUsage.FEATURES.maybeDhHints());
		return new TreeGenerator(
			columns,
			column,
			world,
			blockQueue,
			permuter,
			config.palette.value(),
			config.ground_replacements,
			trunkConfig,
			branchesConfig,
			decorationsBuilder.build(),
			null
		)
		.generate();
	}

	public static record Config(
		Holder<WoodPalette> palette,
		BlockState2ObjectMap<BlockState> ground_replacements,
		TreeHeightScript.Catcher height,
		TrunkFactory trunk,
		Branches branches,
		@VerifyNullable Decorations decorations,
		ColumnRandomYToIntScript.@VerifyNullable Catcher max_saplings
	)
		implements FeatureConfiguration {}

	public static record Branches(
		RandomSource start_frac_y,
		RandomSource count_per_layer,
		Catcher length_function,
		Catcher height_function
	) {}

	public static record Decorations(
		BlockDecorator @VerifyNullable [] trunk,
		BlockDecorator @VerifyNullable [] branches,
		BlockDecorator @VerifyNullable [] leaves,
		@VerifyNullable BallLeaves ball_leaves
	) {

		public static List<BlockDecorator> addAll(
			@NotNull BlockDecorator @Nullable [] toAdd,
			@Nullable List<@NotNull BlockDecorator> addTo
		) {
			if (toAdd != null && toAdd.length != 0) {
				if (addTo == null) addTo = new ArrayList<>(toAdd.length + 2);
				addTo.addAll(Arrays.asList(toAdd));
			}
			return addTo;
		}

		public void addTo(DecoratorConfig.Builder builder) {
			builder.trunkBlock = addAll(this.trunk, builder.trunkBlock);
			builder.branchBlock = addAll(this.branches, builder.branchBlock);
			builder.leafBlock = addAll(this.leaves, builder.leafBlock);
			if (this.ball_leaves != null) {
				BallLeafDecorator decorator = new BallLeafDecorator(this.ball_leaves.inner_state);
				builder.branch(decorator).trunk(decorator);
			}
		}
	}

	public static record BallLeaves(BlockState inner_state) {}

	public static interface TreeHeightScript extends ColumnScript {

		public abstract int getHeight(ScriptedColumn column, double baseRadius, RandomGenerator random);

		@Wrapper
		public static class Catcher extends BaseCatcher<TreeHeightScript> implements TreeHeightScript {

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<TreeHeightScript> getScriptClass() {
				return TreeHeightScript.class;
			}

			@Override
			public void addExtraFunctionsToEnvironment(ImplParameters parameters, ExpressionParser parser) {
				super.addExtraFunctionsToEnvironment(parameters, parser);
				parser.environment.mutable().addVariableLoad("baseRadius", TypeInfos.DOUBLE);
			}

			@Override
			public int getHeight(ScriptedColumn column, double baseRadius, RandomGenerator random) {
				try {
					return this.script.getHeight(column, baseRadius, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}
}