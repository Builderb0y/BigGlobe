package builderb0y.bigglobe.features;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.FeatureColumns;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ColumnYRandomToDoubleScript;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.TreeRegistry;
import builderb0y.bigglobe.trees.TrunkFactory;
import builderb0y.bigglobe.trees.branches.BranchesConfig;
import builderb0y.bigglobe.trees.branches.ScriptedBranchShape;
import builderb0y.bigglobe.trees.decoration.*;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

public class NaturalTreeFeature extends Feature<NaturalTreeFeature.Config> {

	public NaturalTreeFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public NaturalTreeFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		Config config = context.getConfig();
		if (config.delay_generation && DistantHorizonsCompat.isOnDistantHorizonThread()) return false;
		Permuter permuter = Permuter.from(context.getRandom());
		BlockPos origin = context.getOrigin();
		double startX = origin.getX() + Permuter.nextUniformDouble(permuter) * 0.5D;
		int startY = origin.getY();
		double startZ = origin.getZ() + Permuter.nextUniformDouble(permuter) * 0.5D;
		WorldColumn column = FeatureColumns.get(context.getWorld(), origin.getX(), origin.getZ());
		double height = config.height.evaluate(column, origin.getY(), permuter);
		if (!(height > 0.0D)) return false;
		TrunkConfig trunkConfig = config.trunk.create(
			startX,
			startY,
			startZ,
			Math.max(Permuter.roundRandomlyI(permuter, height), 4),
			Math.max(height * 0.125D, TrunkConfig.MIN_RADIUS),
			permuter
		);
		double startFracY = config.branches.start_frac_y.get(permuter);
		BranchesConfig branchesConfig = BranchesConfig.create(
			startFracY,
			Permuter.roundRandomlyI(permuter, config.branches.count_per_layer.get(permuter) * height * (1.0D - startFracY)),
			permuter.nextDouble(BigGlobeMath.TAU),
			trunkConfig.startRadius,
			config.branches.length_function,
			config.branches.height_function
		);

		DecoratorConfig.Builder decoratorsBuilder = new DecoratorConfig.Builder();
		if (config.decorations != null) config.decorations.addTo(decoratorsBuilder);
		if (config.shelves != null && config.shelves.length != 0) {
			RandomList<ShelfPlacer> shelves = new RandomList<>(config.shelves.length);
			for (Shelf shelf : config.shelves) {
				shelves.add(ShelfPlacer.create(shelf.state), shelf.restrictions.getRestriction(column, startY));
			}
			if (shelves.totalWeight > 0.0D) {
				decoratorsBuilder.trunkLayer(new ShelfDecorator(shelves, branchesConfig.startFracY, shelves.totalWeight));
			}
		}
		return new TreeGenerator(
			context.getWorld(),
			config.delay_generation
			? new SerializableBlockQueue(origin.getX(), origin.getY(), origin.getZ(), Block.NOTIFY_LISTENERS | Block.SKIP_LIGHTING_UPDATES)
			: new BlockQueue(false),
			permuter,
			config.palette,
			trunkConfig,
			branchesConfig,
			decoratorsBuilder.build(),
			column
		)
		.generate();
	}

	public static record Config(
		@DefaultBoolean(false) boolean delay_generation,
		TreeRegistry.Entry palette,
		ColumnYRandomToDoubleScript.Holder height,
		TrunkFactory trunk,
		Branches branches,
		Shelf @VerifyNullable [] shelves,
		@VerifyNullable Decorations decorations
	)
	implements FeatureConfig {}

	public static record Branches(
		RandomSource start_frac_y,
		RandomSource count_per_layer,
		ScriptedBranchShape.Holder length_function,
		ScriptedBranchShape.Holder height_function
	) {}

	public static record Shelf(
		BlockState state,
		ColumnRestriction restrictions
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
			if (toAdd != null) {
				if (addTo == null) addTo = new ArrayList<>(toAdd.length + 2);
				for (BlockDecorator decorator : toAdd) {
					addTo.add(decorator.copyIfMutable());
				}
			}
			return addTo;
		}

		public void addTo(DecoratorConfig.Builder builder) {
			builder.trunkBlock  = addAll(this.trunk,    builder. trunkBlock);
			builder.branchBlock = addAll(this.branches, builder.branchBlock);
			builder.leafBlock   = addAll(this.leaves,   builder.  leafBlock);
			if (this.ball_leaves != null) {
				BallLeafDecorator decorator = new BallLeafDecorator(this.ball_leaves.inner_state);
				builder.branch(decorator).trunk(decorator);
			}
		}
	}

	public static record BallLeaves(BlockState inner_state) {}
}