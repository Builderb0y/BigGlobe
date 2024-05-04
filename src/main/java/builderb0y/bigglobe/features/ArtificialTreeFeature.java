package builderb0y.bigglobe.features;

import java.util.*;
import java.util.random.RandomGenerator;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.RandomScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkFactory;
import builderb0y.bigglobe.trees.branches.BranchesConfig;
import builderb0y.bigglobe.trees.branches.ScriptedBranchShape;
import builderb0y.bigglobe.trees.decoration.BlockDecorator;
import builderb0y.bigglobe.trees.decoration.DecoratorConfig;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;
import builderb0y.bigglobe.util.Directions;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ArtificialTreeFeature extends Feature<ArtificialTreeFeature.Config> {

	public ArtificialTreeFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ArtificialTreeFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		if (!(context.getGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return false;
		StructureWorldAccess world = context.getWorld();
		Config config = context.getConfig();
		IRandomList<Block> saplingBlocks = config.palette.value().saplingBlocks();
		BlockPos origin = context.getOrigin();
		if (!saplingBlocks.contains(world.getBlockState(origin).getBlock())) return false;
		Permuter permuter = Permuter.from(context.getRandom());
		BlockQueue blockQueue = new BlockQueue(true);
		Deque<BlockPos> toCheck = new ArrayDeque<>(8);
		blockQueue.queueBlock(origin, BlockStates.AIR);
		toCheck.add(origin);
		double centerX = origin.getX();
		int centerY = origin.getY();
		double centerZ = origin.getZ();
		outer:
		for (BlockPos pos; (pos = toCheck.pollFirst()) != null;) {
			for (Direction direction : Directions.HORIZONTAL) {
				BlockPos offset = pos.offset(direction);
				if (blockQueue.getBlockStateOrNull(offset) == null && saplingBlocks.contains(world.getBlockState(offset).getBlock())) {
					blockQueue.queueBlock(offset, BlockStates.AIR);
					centerX += offset.getX();
					centerZ += offset.getZ();
					if (blockQueue.blockCount() >= 1024) break outer;
					toCheck.addLast(offset);
				}
			}
		}
		double saplingCount = blockQueue.blockCount();
		centerX /= saplingCount;
		centerZ /= saplingCount;
		centerX += Permuter.nextUniformDouble(permuter) * 0.5D;
		centerZ += Permuter.nextUniformDouble(permuter) * 0.5D;
		double baseRadius = Math.sqrt(saplingCount / Math.PI);
		int trunkHeight = config.height.getHeight(baseRadius, permuter);
		if (trunkHeight <= 0) return false;
		TrunkConfig trunkConfig = config.trunk.create(
			centerX,
			centerY,
			centerZ,
			trunkHeight,
			permuter
		);
		double startFracY = config.branches.start_frac_y.get(permuter);
		BranchesConfig branchesConfig = BranchesConfig.create(
			startFracY,
			Permuter.roundRandomlyI(permuter, config.branches.count_per_layer.get(permuter) * trunkHeight * (1.0D - startFracY)),
			permuter.nextDouble(BigGlobeMath.TAU),
			trunkConfig.baseRadius,
			config.branches.length_function,
			config.branches.height_function
		);
		DecoratorConfig.Builder decorationsBuilder = new DecoratorConfig.Builder();
		if (config.decorations != null) config.decorations.addTo(decorationsBuilder);
		ScriptedColumn column = generator.newColumn(world, BigGlobeMath.floorI(centerX), BigGlobeMath.floorI(centerZ), Purpose.GENERIC);

		return new TreeGenerator(
			world,
			blockQueue,
			permuter,
			config.palette.value(),
			config.ground_replacements,
			trunkConfig,
			branchesConfig,
			decorationsBuilder.build(),
			column
		)
		.generate();
	}

	public static record Config(
		RegistryEntry<WoodPalette> palette,
		Map<BlockState, BlockState> ground_replacements,
		TreeHeightScript.Holder height,
		TrunkFactory trunk,
		Branches branches,
		@VerifyNullable Decorations decorations
	)
	implements FeatureConfig {}

	public static record Branches(
		RandomSource start_frac_y,
		RandomSource count_per_layer,
		ScriptedBranchShape.Holder length_function,
		ScriptedBranchShape.Holder height_function
	) {}

	public static record Decorations(
		BlockDecorator @VerifyNullable [] trunk,
		BlockDecorator @VerifyNullable [] branches,
		BlockDecorator @VerifyNullable [] leaves
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
		}
	}

	public static interface TreeHeightScript extends Script {

		public abstract int getHeight(double baseRadius, RandomGenerator random);

		@Wrapper
		public static class Holder extends ScriptHolder<TreeHeightScript> implements TreeHeightScript {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				this.script = (
					new TemplateScriptParser<>(TreeHeightScript.class, this.usage)
					.addEnvironment(MathScriptEnvironment.INSTANCE)
					.addEnvironment(RandomScriptEnvironment.create(
						load("random", type(RandomGenerator.class))
					))
					.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
					.configureEnvironment((MutableScriptEnvironment environment) -> {
						environment.addVariableLoad("baseRadius", TypeInfos.DOUBLE);
					})
					.parse(new ScriptClassLoader())
				);
			}

			@Override
			public boolean requiresColumns() {
				return false;
			}

			@Override
			public int getHeight(double baseRadius, RandomGenerator random) {
				try {
					return this.script.getHeight(baseRadius, random);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}
}