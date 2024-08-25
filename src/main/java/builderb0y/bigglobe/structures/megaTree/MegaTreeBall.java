package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToDoubleScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.DataStructurePiece;
import builderb0y.bigglobe.structures.megaTree.MegaTreeBall.Data;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreeBall extends DataStructurePiece<Data> {

	public static record Data(
		RegistryEntry<Structure> structure,
		@Hidden MegaTreeStructure actualStructure,
		double x,
		double y,
		double z,
		double radius,
		int step,
		int totalSteps,
		RegistryEntry<WoodPalette> wood
	) {

		public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

		public Data {
			if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isNaN(radius)) {
				throw new IllegalArgumentException("Attempt to create tree ball with NaN position or radius.");
			}
		}

		public Data(RegistryEntry<Structure> structure, double x, double y, double z, double radius, int step, int totalSteps, RegistryEntry<WoodPalette> wood) {
			this(structure, (MegaTreeStructure)(structure.value()), x, y, z, radius, step, totalSteps, wood);
		}

		public Data(MegaTreeStructure actualStructure, double x, double y, double z, double radius, int step, int totalSteps, RegistryEntry<WoodPalette> wood) {
			this(getActualEntry(actualStructure), actualStructure, x, y, z, radius, step, totalSteps, wood);
		}

		public static RegistryEntry<Structure> getActualEntry(MegaTreeStructure structure) {
			return BigGlobeMod.getCurrentServer().getRegistryManager().get(RegistryKeyVersions.structure()).getEntry(structure);
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
		double x,
		double y,
		double z,
		double radius,
		int currentStep,
		int totalSteps
	) {
		super(
			type,
			0,
			null,
			new Data(structure, x, y, z, radius, currentStep, totalSteps, structure.data.palette())
		);
		double extraLeafRadius = this.data.extraLeafRadius();
		double totalRadius = radius + extraLeafRadius;
		this.boundingBox = WorldUtil.createBlockBox(
			ceilI(x - totalRadius),
			ceilI(y - totalRadius),
			ceilI(z - totalRadius),
			floorI(x + totalRadius),
			floorI(y + totalRadius),
			floorI(z + totalRadius)
		);
	}

	public MegaTreeBall(
		StructurePieceType type,
		MegaTreeStructure structure,
		MegaTreeBranch branch,
		Vector3d position,
		double radius
	) {
		this(
			type,
			structure,
			position.x,
			position.y,
			position.z,
			radius,
			branch.currentStep,
			branch.totalSteps
		);
	}

	public MegaTreeBall(StructurePieceType type, StructureContext context, NbtCompound nbt) {
		super(type, context, nbt);
	}

	@Override
	public AutoCoder<Data> dataCoder() {
		return Data.CODER;
	}

	@Override
	public void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		ColumnToDoubleScript.Holder snowChance = this.data.actualStructure.data.snow_chance();
		ScriptedColumn column = snowChance != null && chunkGenerator instanceof BigGlobeScriptedChunkGenerator scriptedGenerator ? scriptedGenerator.newColumn(world, 0, 0, Purpose.generic()) : null;

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
			minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX()),
			minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ()),
			maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX()),
			maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());

		WoodPalette palette = this.data.wood.value();
		boolean placedAnyLogs = false;
		for (pos.setX(minX); pos.getX() <= maxX; pos.setX(pos.getX() + 1)) {
			double xSquared = squareD(pos.getX() - centerX);
			for (pos.setZ(minZ); pos.getZ() <= maxZ; pos.setZ(pos.getZ() + 1)) {
				double xzSquared = xSquared + squareD(pos.getZ() - centerZ);
				if (xzSquared < radiusSquared) {
					double chord = Math.sqrt(radiusSquared - xzSquared);
					int minY = Math.max(ceilI(centerY - chord), chunkBox.getMinY());
					int maxY = Math.min(floorI(centerY + chord), chunkBox.getMaxY());
					for (pos.setY(maxY); pos.getY() >= minY; pos.setY(pos.getY() - 1)) {
						if (this.canLogReplace(world.getBlockState(pos))) {
							world.setBlockState(pos, palette.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							placedAnyLogs = true;
						}
						else {
							break;
						}
					}
					for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
						if (this.canLogReplace(world.getBlockState(pos))) {
							world.setBlockState(pos, palette.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
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
			if (chunkBox.contains(pos)) {
				world.setBlockState(pos, palette.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
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
						world.setBlockState(pos, palette.leavesState(permuter, 7, true, false), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
					this.placeSnow(world, pos.setY(topY + 1), column, snowChance, permuter);
				}
			}
		}
		//*/
	}

	public boolean canLogReplace(BlockState state) {
		return BlockStateVersions.isReplaceable(state) || state.isIn(BigGlobeBlockTags.TREE_LOG_REPLACEABLES);
	}

	public boolean canLeavesReplace(BlockState state) {
		return state.isAir() || state.getBlock() instanceof SnowBlock;
	}

	public void placeSnow(StructureWorldAccess world, BlockPos.Mutable pos, ScriptedColumn column, ColumnToDoubleScript.Holder snowChance, Permuter permuter) {
		if (column != null) {
			column.setParamsUnchecked(column.params.at(pos.getX(), pos.getZ()));
			if (world.isAir(pos) && Permuter.nextChancedBoolean(permuter, snowChance.get(column))) {
				world.setBlockState(pos, BlockStates.SNOW, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}
	}

	public static BlockPos.Mutable setToRound(BlockPos.Mutable pos, double x, double y, double z) {
		return pos.set(roundI(x), roundI(y), roundI(z));
	}

	@Override
	public String toString() {
		return "MegaTreeBall(" + this.data + ')';
	}
}