package builderb0y.bigglobe.structures.megaTree;

import java.util.ArrayDeque;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.EncodeInline;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnRandomYToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnYToWoodPaletteScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.entries.WoodPaletteEntry;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.megaTree.MegaTreePiece.BallCollection;

import static builderb0y.bigglobe.math.BigGlobeMath.floorI;

public class MegaTreeStructure extends BigGlobeStructure {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<MegaTreeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MegaTreeStructure.class);
	#else
		public static final Codec<MegaTreeStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(MegaTreeStructure.class).codec();
	#endif

	public static record Data(
		ColumnRandomYToDoubleScript.Holder size,
		ColumnRandomYToDoubleScript.Holder trunk_radius,
		ColumnRandomYToDoubleScript.Holder branch_sparsity,
		ColumnToDoubleScript.@VerifyNullable Holder snow_chance,
		ColumnYToWoodPaletteScript.Holder palette,
		int max_radius_in_chunks
	) {}

	public final @EncodeInline Data data;

	public MegaTreeStructure(Config config, ColumnToIntScript.@VerifyNullable Holder surface_y, Data data) {
		super(config, null, surface_y);
		this.data = data;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return this.data.max_radius_in_chunks;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		if (!(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return Optional.empty();
		long seed = chunkSeed(context, 0x462E8B50AE715A33L);
		double x = context.chunkPos().getStartX() + context.random().nextDouble() * 16.0D;
		double z = context.chunkPos().getStartZ() + context.random().nextDouble() * 16.0D;
		ScriptedColumn column = generator.newColumn(context.world(), floorI(x), floorI(z), ColumnUsage.GENERIC.maybeDhHints());
		double y = (this.surface_y != null ? this.surface_y.get(column) : context.chunkGenerator().getHeightInGround(floorI(x), floorI(z), Heightmap.Type.OCEAN_FLOOR_WG, context.world(), context.noiseConfig())) + 1;
		WoodPaletteEntry palette = this.data.palette.get(column, floorI(y));
		if (palette == null) return Optional.empty();
		Permuter permuter = new Permuter(seed);
		double size = this.data.size.get(column, permuter, floorI(y));
		permuter.setSeed(seed);
		double trunkRadius = this.data.trunk_radius.get(column, permuter, floorI(y));
		permuter.setSeed(seed);
		double branchSparsity = this.data.branch_sparsity.get(column, permuter, floorI(y));
		permuter.setSeed(seed);
		return Optional.of(
			new StructurePosition(
				BlockPos.ofFloored(x, y, z),
				(StructurePiecesCollector collector) -> {
					MegaTreeContext megaTreeContext = new MegaTreeContext(
						this,
						context,
						permuter,
						column,
						palette.entry,
						size,
						trunkRadius,
						branchSparsity,
						new MegaTreeOctree(
							x - size,
							y - size,
							z - size,
							x + size,
							y + size,
							z + size
						),
						x,
						y,
						z
					);
					megaTreeContext.addFirstBranch();
					megaTreeContext.generate();
					collector.addPiece(
						new MegaTreePiece(
							BigGlobeStructures.MEGA_TREE_PIECE_TYPE,
							this,
							palette.entry,
							megaTreeContext.ballCollector
						)
					);
				}
			)
		);
	}

	public static class MegaTreeContext {

		public final MegaTreeStructure structure;
		public final Structure.Context structureContext;
		public final Permuter permuter;
		public final ScriptedColumn column;
		public final RegistryEntry<WoodPalette> palette;
		public final double size, trunkRadius, branchSparsity;
		public final MegaTreeOctree octree;
		public final BallCollection ballCollector;
		public final ArrayDeque<MegaTreeBranch> branches;
		public final ArrayDeque<Ball> currentBranchBalls;

		public MegaTreeContext(
			MegaTreeStructure structure,
			Structure.Context structureContext,
			Permuter permuter,
			ScriptedColumn column,
			RegistryEntry<WoodPalette> palette,
			double size,
			double trunkRadius,
			double branchSparsity,
			MegaTreeOctree octree,
			double x,
			double y,
			double z
		) {
			this.structure          = structure;
			this.structureContext   = structureContext;
			this.permuter           = permuter;
			this.column             = column;
			this.palette            = palette;
			this.size               = size;
			this.trunkRadius        = trunkRadius;
			this.branchSparsity     = branchSparsity;
			this.octree             = octree;
			this.ballCollector      = new BallCollection(x, y, z);
			this.branches           = new ArrayDeque<>(256);
			this.currentBranchBalls = new ArrayDeque<>(256);
		}

		public void addBall(Ball ball) {
			this.currentBranchBalls.addLast(ball);
		}

		public void addBranch(MegaTreeBranch branch) {
			this.branches.addLast(branch);
		}

		public void addFirstBranch() {
			int totalSteps = (int)(this.size);
			MegaTreeBranch branch = new MegaTreeBranch(
				this,
				this.ballCollector.originX,
				this.ballCollector.originY,
				this.ballCollector.originZ,
				this.trunkRadius,
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
				for (Ball ball; (ball = this.currentBranchBalls.pollFirst()) != null;) {
					this.octree.addBall(ball);
					this.ballCollector.accept(ball);
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