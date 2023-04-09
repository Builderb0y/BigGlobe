package builderb0y.bigglobe.structures.megaTree;

import java.util.ArrayDeque;
import java.util.Optional;

import com.mojang.serialization.Codec;
import org.joml.Vector3d;

import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.trees.TreeRegistry;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

public class MegaTreeStructure extends BigGlobeStructure {

	public static final Codec<MegaTreeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(MegaTreeStructure.class);

	public static record FoliageRange(double min, double max) {

		public double get(double foliage) {
			return Interpolator.mixLinear(this.min, this.max, foliage);
		}
	}
	public static record Data(
		FoliageRange size,
		FoliageRange trunk_radius,
		FoliageRange branch_sparsity,
		TreeRegistry.Entry palette
	) {}

	public final @EncodeInline Data data;

	public MegaTreeStructure(Config config, Data data) {
		super(config);
		this.data = data;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		long seed = chunkSeed(context, 0x462E8B50AE715A33L);
		double x = context.chunkPos().getStartX() + context.random().nextDouble() * 16.0D;
		double z = context.chunkPos().getStartZ() + context.random().nextDouble() * 16.0D;
		WorldColumn column = WorldColumn.forGenerator(
			context.seed(),
			context.chunkGenerator(),
			context.noiseConfig(),
			floorI(x),
			floorI(z)
		);
		double y = column.getFinalTopHeightD();
		double foliage = ColumnValue.OVERWORLD_SURFACE_FOLIAGE.getValueWithoutY(column);
		if (Double.isNaN(foliage)) foliage = Permuter.nextPositiveFloat(seed);
		double foliage_ = foliage; //lambdas -_-
		return Optional.of(
			new StructurePosition(
				BlockPos.ofFloored(x, y, z),
				(StructurePiecesCollector collector) -> {
					double size = this.data.size.get(foliage_);
					MegaTreeContext megaTreeContext = new MegaTreeContext(
						new Permuter(seed),
						column,
						foliage_,
						this.data,
						new MegaTreeOctree(
							x - size,
							y - size,
							z - size,
							x + size,
							y + size,
							z + size
						),
						collector
					);
					megaTreeContext.addFirstBranch(x, y, z);
					megaTreeContext.generate();
				}
			)
		);
	}

	public static class MegaTreeContext {

		public final Permuter permuter;
		public final WorldColumn column;
		public final double foliage;
		public final Data data;
		public final MegaTreeOctree octree;
		public final StructurePiecesCollector ballCollector;
		public final ArrayDeque<MegaTreeBranch> branches;
		public final ArrayDeque<MegaTreeBall> currentBranchBalls;

		public MegaTreeContext(
			Permuter permuter,
			WorldColumn column,
			double foliage,
			Data data,
			MegaTreeOctree octree,
			StructurePiecesCollector ballCollector
		) {
			this.permuter           = permuter;
			this.column             = column;
			this.foliage            = foliage;
			this.data               = data;
			this.octree             = octree;
			this.ballCollector      = ballCollector;
			this.branches           = new ArrayDeque<>(256);
			this.currentBranchBalls = new ArrayDeque<>(256);
		}

		public double foliageFactor(FoliageRange range) {
			return range.get(this.foliage);
		}

		public void addBall(MegaTreeBall ball) {
			this.currentBranchBalls.addLast(ball);
		}

		public void addBranch(MegaTreeBranch branch) {
			this.branches.addLast(branch);
		}

		public void addFirstBranch(double x, double y, double z) {
			int totalSteps = (int)(this.foliageFactor(this.data.size));
			MegaTreeBranch branch = new MegaTreeBranch(
				this,
				x,
				y,
				z,
				this.foliageFactor(this.data.trunk_radius),
				totalSteps,
				32,
				new Vector3d(0.0D, 1.0D, 0.0D),
				new Vector3d(0.0D, 1.0D, 0.0D)
			);
			this.branches.addLast(branch);
		}

		public void generate() {
			//long startTime = System.currentTimeMillis();
			for (MegaTreeBranch branch; (branch = this.branches.pollFirst()) != null;) {
				branch.generate();
				for (MegaTreeBall ball; (ball = this.currentBranchBalls.pollFirst()) != null;) {
					this.octree.addBall(ball);
					this.ballCollector.addPiece(ball);
				}
			}
			//long endTime = System.currentTimeMillis();
			//System.out.println("Generated in " + (endTime - startTime) + " ms");
		}
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.MEGA_TREE_TYPE;
	}
}