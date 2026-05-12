package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.DataStructurePiece;
import builderb0y.bigglobe.structures.management.StructureLocator;
import builderb0y.bigglobe.structures.megaTree.MegaTreeBall.Data;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

//deprecated in favor of MegaTreePiece, which is more efficient for chunk serialization.
//this class will be preserved to prevent old worlds from spamming log files.
@Deprecated
public class MegaTreeBall extends DataStructurePiece<Data> {

	public static record Data(
		Holder<Structure> structure,
		@Hidden MegaTreeStructure actualStructure,
		double x,
		double y,
		double z,
		double radius,
		int step,
		int totalSteps,
		Holder<WoodPalette> wood
	) {

		public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

		public Data {
			if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isNaN(radius)) {
				throw new IllegalArgumentException("Attempt to create tree ball with NaN position or radius.");
			}
		}

		public Data(Holder<Structure> structure, double x, double y, double z, double radius, int step, int totalSteps, Holder<WoodPalette> wood) {
			this(structure, (MegaTreeStructure)(structure.value()), x, y, z, radius, step, totalSteps, wood);
		}

		public Data(MegaTreeStructure actualStructure, double x, double y, double z, double radius, int step, int totalSteps, Holder<WoodPalette> wood) {
			this(StructureLocator.toHolder(actualStructure), actualStructure, x, y, z, radius, step, totalSteps, wood);
		}

		public Vector3d position() {
			return new Vector3d(this.x, this.y, this.z);
		}

		public double extraLeafRadius() {
			return 64.0D / (this.totalSteps - this.step + 32);
		}
	}

	public MegaTreeBall(
		StructurePieceType type,
		MegaTreeStructure structure,
		Holder<WoodPalette> palette,
		double x,
		double y,
		double z,
		double radius,
		int currentStep,
		int totalSteps
	) {
		this(type, new Data(structure, x, y, z, radius, currentStep, totalSteps, palette));
	}

	public MegaTreeBall(
		StructurePieceType type,
		Data data
	) {
		super(type, 0, null, data);
		double extraLeafRadius = this.data.extraLeafRadius();
		double totalRadius = data.radius + extraLeafRadius;
		this.boundingBox = WorldUtil.createBlockBox(
			ceilI(data.x - totalRadius),
			ceilI(data.y - totalRadius),
			ceilI(data.z - totalRadius),
			floorI(data.x + totalRadius),
			floorI(data.y + totalRadius),
			floorI(data.z + totalRadius)
		);
	}

	public MegaTreeBall(
		StructurePieceType type,
		MegaTreeStructure structure,
		Holder<WoodPalette> palette,
		MegaTreeBranch branch,
		Vector3d position,
		double radius
	) {
		this(
			type,
			structure,
			palette,
			position.x,
			position.y,
			position.z,
			radius,
			branch.currentStep,
			branch.totalSteps
		);
	}

	public MegaTreeBall(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
		super(type, context, nbt);
	}

	@Override
	public AutoCoder<Data> dataCoder() {
		return Data.CODER;
	}

	@Override
	public void postProcess(
		WorldGenLevel world,
		StructureManager structureAccessor,
		ChunkGenerator chunkGenerator,
		RandomSource random,
		BoundingBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		ColumnToDoubleScript.Catcher snowChance = this.data.actualStructure.data.snow_chance();
		ScriptedColumn column = snowChance != null && chunkGenerator instanceof BigGlobeScriptedChunkGenerator scriptedGenerator ? scriptedGenerator.newColumn(world, 0, 0, ColumnUsage.GENERIC.maybeDhHints()) : null;

		double
			centerX = this.data.x,
			centerY = this.data.y,
			centerZ = this.data.z,
			radiusSquared = squareD(this.data.radius);
		long seed = Permuter.permute(
			world.getSeed() ^ 0x723173E214442521L,
			centerX,
			centerY,
			centerZ
		);
		Permuter permuter = new Permuter(seed);
		int
			minX = Math.max(this.boundingBox.minX(), chunkBox.minX()),
			minZ = Math.max(this.boundingBox.minZ(), chunkBox.minZ()),
			maxX = Math.min(this.boundingBox.maxX(), chunkBox.maxX()),
			maxZ = Math.min(this.boundingBox.maxZ(), chunkBox.maxZ());

		WoodPalette palette = this.data.wood.value();
		boolean placedAnyLogs = false;
		for (pos.setX(minX); pos.getX() <= maxX; pos.setX(pos.getX() + 1)) {
			double xSquared = squareD(pos.getX() - centerX);
			for (pos.setZ(minZ); pos.getZ() <= maxZ; pos.setZ(pos.getZ() + 1)) {
				double xzSquared = xSquared + squareD(pos.getZ() - centerZ);
				if (xzSquared < radiusSquared) {
					double chord = Math.sqrt(radiusSquared - xzSquared);
					int minY = Math.max(ceilI(centerY - chord), chunkBox.minY());
					int maxY = Math.min(floorI(centerY + chord), chunkBox.maxY());
					for (pos.setY(maxY); pos.getY() >= minY; pos.setY(pos.getY() - 1)) {
						if (this.canLogReplace(world.getBlockState(pos))) {
							world.setBlock(pos, palette.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
							placedAnyLogs = true;
						}
						else {
							break;
						}
					}
					for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
						if (this.canLogReplace(world.getBlockState(pos))) {
							world.setBlock(pos, palette.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
							placedAnyLogs = true;
						}
						else {
							break;
						}
					}
					this.placeSnow(world, pos.setY(maxY + 1), column, snowChance, permuter);
				}
			}
		}
		if (!placedAnyLogs) {
			setToRound(pos, centerX, centerY, centerZ);
			if (chunkBox.isInside(pos)) {
				world.setBlock(pos, palette.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}

		//*
		double extraLeafRadius = this.data.extraLeafRadius();
		int leafCount = Permuter.roundRandomlyI(permuter.nextLong(), squareD(extraLeafRadius * 2.0));
		if (leafCount > 0) {
			Vector3d unitGenerator = new Vector3d();
			for (int i = 0; i < leafCount; i++) {
				Vectors.setInSphere(
						unitGenerator,
						permuter,
						permuter.nextDouble() * extraLeafRadius + this.data.radius
					)
					.add(centerX, centerY, centerZ);
				setToRound(pos, unitGenerator.x, unitGenerator.y, unitGenerator.z);
				if (pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
					int topY = pos.getY();
					for (
						int bits = permuter.nextInt() | 1;
						(bits & 1) != 0 && this.canLeavesReplace(world.getBlockState(pos));
						bits >>>= 1, pos.setY(pos.getY() - 1)
					) {
						world.setBlock(pos, palette.leavesState(permuter, 7, true, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					}
					this.placeSnow(world, pos.setY(topY + 1), column, snowChance, permuter);
				}
			}
		}
		//*/
	}

	public boolean canLogReplace(BlockState state) {
		return BlockStateVersions.isReplaceable(state) || state.is(BigGlobeBlockTags.TREE_LOG_REPLACEABLES);
	}

	public boolean canLeavesReplace(BlockState state) {
		return state.isAir() || state.getBlock() instanceof SnowLayerBlock;
	}

	public void placeSnow(WorldGenLevel world, BlockPos.MutableBlockPos pos, ScriptedColumn column, ColumnToDoubleScript.Catcher snowChance, Permuter permuter) {
		if (column != null) {
			column.setParamsUnchecked(column.params.at(pos.getX(), pos.getZ()));
			if (world.isEmptyBlock(pos) && Permuter.nextChancedBoolean(permuter, snowChance.get(column))) {
				world.setBlock(pos, BlockStates.SNOW, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			}
		}
	}

	public static BlockPos.MutableBlockPos setToRound(BlockPos.MutableBlockPos pos, double x, double y, double z) {
		return pos.set(roundI(x), roundI(y), roundI(z));
	}

	@Override
	public String toString() {
		return "MegaTreeBall(" + this.data + ')';
	}
}