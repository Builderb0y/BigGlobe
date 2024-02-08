package builderb0y.bigglobe.structures.megaTree;

import java.util.ArrayDeque;
import java.util.Optional;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.versions.BlockPosVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

public class MegaTreeStructure extends BigGlobeStructure {

	public static final Codec<MegaTreeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(MegaTreeStructure.class);

	public static record FoliageRange(double min, double max) {

		public double get(double foliage) {
			return Interpolator.mixLinear(this.min, this.max, foliage);
		}
	}
	public static record Data(
		ColumnToDoubleScript.@VerifyNullable Holder surface_y,
		ColumnToDoubleScript.@VerifyNullable Holder foliage,
		ColumnToDoubleScript.@VerifyNullable Holder snow_chance,
		FoliageRange size,
		FoliageRange trunk_radius,
		FoliageRange branch_sparsity,
		RegistryEntry<WoodPalette> palette
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
		ScriptedColumn column = context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator ? generator.newColumn(context.world(), floorI(x), floorI(z), DistantHorizonsCompat.isOnDistantHorizonThread()) : null;
		double y = column != null && this.data.surface_y != null ? this.data.surface_y.get(column) : context.chunkGenerator().getHeightOnGround(floorI(x), floorI(z), Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig());
		double foliage = column != null && this.data.foliage != null ? this.data.foliage.get(column) : 0.0D;
		return Optional.of(
			new StructurePosition(
				BlockPosVersions.floor(x, y, z),
				(StructurePiecesCollector collector) -> {
					double size = this.data.size.get(foliage);
					MegaTreeContext megaTreeContext = new MegaTreeContext(
						this,
						context,
						new Permuter(seed),
						column,
						foliage,
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

		public final MegaTreeStructure structure;
		public final Structure.Context structureContext;
		public final Permuter permuter;
		public final @Nullable ScriptedColumn column;
		public final double foliage;
		public final MegaTreeOctree octree;
		public final StructurePiecesCollector ballCollector;
		public final ArrayDeque<MegaTreeBranch> branches;
		public final ArrayDeque<MegaTreeBall> currentBranchBalls;

		public MegaTreeContext(
			MegaTreeStructure structure,
			Structure.Context structureContext,
			Permuter permuter,
			@Nullable ScriptedColumn column,
			double foliage,
			MegaTreeOctree octree,
			StructurePiecesCollector ballCollector
		) {
			this.structure          = structure;
			this.structureContext   = structureContext;
			this.permuter           = permuter;
			this.column             = column;
			this.foliage            = foliage;
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
			int totalSteps = (int)(this.foliageFactor(this.structure.data.size));
			MegaTreeBranch branch = new MegaTreeBranch(
				this,
				x,
				y,
				z,
				this.foliageFactor(this.structure.data.trunk_radius),
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