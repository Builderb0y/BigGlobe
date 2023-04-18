package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.DataStructurePiece;
import builderb0y.bigglobe.structures.megaTree.MegaTreeBall.Data;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.util.WorldUtil;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreeBall extends DataStructurePiece<Data> {

	public static record Data(double x, double y, double z, double radius, int step, int totalSteps, RegistryEntry<WoodPalette> wood) {

		public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

		public Vector3d position() {
			return new Vector3d(this.x, this.y, this.z);
		}

		public double extraLeafRadius() {
			return 64.0D / (this.totalSteps - this.step + 32);
		}
	}

	public MegaTreeBall(
		StructurePieceType type,
		double x,
		double y,
		double z,
		double radius,
		int currentStep,
		int totalSteps,
		RegistryEntry<WoodPalette> woodPalette
	) {
		super(
			type,
			0,
			null,
			new Data(x, y, z, radius, currentStep, totalSteps, woodPalette)
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
		MegaTreeBranch branch,
		Vector3d position,
		double radius
	) {
		this(
			type,
			position.x,
			position.y,
			position.z,
			radius,
			branch.currentStep,
			branch.totalSteps,
			branch.context.data.palette()
		);
	}

	public MegaTreeBall(StructurePieceType type, NbtCompound nbt) {
		super(type, nbt);
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
		WorldColumn column = WorldColumn.forWorld(world, 0, 0);
		OverworldColumn overworldColumn = column instanceof OverworldColumn ? ((OverworldColumn)(column)) : null;

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

		BlockState wood = this.data.wood.value().woodState(Axis.Y);
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
							world.setBlockState(pos, wood, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							placedAnyLogs = true;
						}
						else {
							break;
						}
					}
					for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
						if (this.canLogReplace(world.getBlockState(pos))) {
							world.setBlockState(pos, wood, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							placedAnyLogs = true;
						}
						else {
							break;
						}
					}
					this.placeSnow(world, pos.setY(maxY + 1), overworldColumn, permuter);
				}
			}
		}
		if (!placedAnyLogs) {
			setToRound(pos, centerX, centerY, centerZ);
			if (chunkBox.contains(pos)) {
				world.setBlockState(pos, wood, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}

		//*
		BlockState leaves = this.data.wood.value().leavesState(7, true, false);
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
						world.setBlockState(pos, leaves, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
					this.placeSnow(world, pos.setY(topY + 1), overworldColumn, permuter);
				}
			}
		}
		//*/
	}

	public boolean canLogReplace(BlockState state) {
		Material material = state.getMaterial();
		return material.isReplaceable() || material == Material.LEAVES;
	}

	public boolean canLeavesReplace(BlockState state) {
		Material material = state.getMaterial();
		return material == Material.AIR || material == Material.SNOW_LAYER;
	}

	public void placeSnow(StructureWorldAccess world, BlockPos.Mutable pos, OverworldColumn overworldColumn, Permuter permuter) {
		if (overworldColumn != null) {
			overworldColumn.setPosUnchecked(pos.getX(), pos.getZ());
			if (world.isAir(pos) && Permuter.nextChancedBoolean(permuter, overworldColumn.getSnowChance())) {
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