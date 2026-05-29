package builderb0y.bigglobe.structures.megaTree;

import java.util.Arrays;
import java.util.function.Consumer;

import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
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
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.versions.BlockStateVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreePiece extends DataStructurePiece<MegaTreePiece.Data> {

	public static class Data {

		public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);
		public Holder<Structure> structure;
		@Hidden
		public MegaTreeStructure actualStructure;
		public Holder<WoodPalette> wood;
		public double originX;
		public double originY;
		public double originZ;
		public float[] balls;

		@Hidden
		public Data(
			Holder<Structure> structure,
			MegaTreeStructure actualStructure,
			Holder<WoodPalette> wood,
			double originX,
			double originY,
			double originZ,
			float[] balls
		) {
			this.structure = structure;
			this.actualStructure = actualStructure;
			this.wood = wood;
			this.originX = originX;
			this.originY = originY;
			this.originZ = originZ;
			this.balls = balls;
		}

		public Data(Holder<Structure> structure, Holder<WoodPalette> wood, double originX, double originY, double originZ, float[] balls) {
			this(structure, ((MegaTreeStructure)(structure.value())), wood, originX, originY, originZ, balls);
		}

		@Hidden
		public Data(MegaTreeStructure actualStructure, Holder<WoodPalette> wood, double originX, double originY, double originZ, float[] balls) {
			this(StructureLocator.toHolder(actualStructure), actualStructure, wood, originX, originY, originZ, balls);
		}

		public int countBalls() {
			return this.balls.length >> 2;
		}

		public double getX(int index) {
			return this.originX + this.balls[index << 2];
		}

		public double getY(int index) {
			return this.originY + this.balls[(index << 2) | 1];
		}

		public double getZ(int index) {
			return this.originZ + this.balls[(index << 2) | 2];
		}

		public double getRadius(int index) {
			return this.balls[(index << 2) | 3];
		}
	}

	@Override
	public void move(int x, int y, int z) {
		super.move(x, y, z);
		this.data.originX += x;
		this.data.originY += y;
		this.data.originZ += z;
	}

	public MegaTreePiece(
		StructurePieceType type,
		MegaTreeStructure structure,
		Holder<WoodPalette> wood,
		BallCollection balls
	) {
		super(
			type,
			0,
			balls.getBox(),
			new Data(
				structure,
				wood,
				balls.originX,
				balls.originY,
				balls.originZ,
				balls.getBalls()
			)
		);
	}

	public MegaTreePiece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
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
		Permuter permuter = new Permuter(Permuter.permute(world.getSeed() ^ 0xD6B4371F4701555BL, chunkBox.minX() >> 4, chunkBox.minY() >> 4, chunkBox.minZ() >> 4));
		WoodPalette wood = this.data.wood.value();
		Vector3d unitGenerator = new Vector3d();

		for (int index = 0, count = this.data.countBalls(); index < count; index++) {
			double
				centerX = this.data.getX(index),
				centerY = this.data.getY(index),
				centerZ = this.data.getZ(index),
				radius = this.data.getRadius(index),
				radiusSquared = radius * radius;
			int
				minX = Math.max(ceilI(centerX - radius), chunkBox.minX()),
				minZ = Math.max(ceilI(centerZ - radius), chunkBox.minZ()),
				maxX = Math.min(floorI(centerX + radius), chunkBox.maxX()),
				maxZ = Math.min(floorI(centerZ + radius), chunkBox.maxZ());
			boolean
				placedAnyLogs = false;
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
								world.setBlock(pos, wood.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
								placedAnyLogs = true;
							}
							else {
								break;
							}
						}
						for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
							if (this.canLogReplace(world.getBlockState(pos))) {
								world.setBlock(pos, wood.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
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
					world.setBlock(pos, wood.woodState(permuter, Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}

			double leafExtra = 2.0D - radius;
			if (leafExtra > 0.0D) {
				int leafCount = Permuter.roundRandomlyI(permuter, squareD(leafExtra * 2.0D));
				if (leafCount > 0) {
					for (int leafIndex = 0; leafIndex < leafCount; leafIndex++) {
						Vectors.setOnSphere(
								unitGenerator,
								permuter,
								permuter.nextDouble() * leafExtra + radius
							)
							.add(centerX, centerY, centerZ);
						setToRound(pos, unitGenerator.x, unitGenerator.y, unitGenerator.z);
						if (
							pos.getX() >= chunkBox.minX() &&
							pos.getX() <= chunkBox.maxX() &&
							pos.getZ() >= chunkBox.minZ() &&
							pos.getZ() <= chunkBox.maxZ()
						) {
							int topY = pos.getY();
							for (
								int bits = permuter.nextInt() | 1;
								(bits & 1) != 0 && this.canLeavesReplace(world.getBlockState(pos));
								bits >>>= 1, pos.setY(pos.getY() - 1)
							) {
								world.setBlock(pos, wood.leavesState(permuter, 7, true, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
							}
							this.placeSnow(world, pos.setY(topY + 1), column, snowChance, permuter);
						}
					}
				}
			}
		}
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

	public static class BallCollection implements Consumer<Ball> {

		public final double originX, originY, originZ;
		public double minX, minY, minZ, maxX, maxY, maxZ;
		public float[] balls = new float[256];
		public int ballIndex;

		public BallCollection(double originX, double originY, double originZ) {
			this.originX = originX;
			this.originY = originY;
			this.originZ = originZ;
			this.minX = this.maxX = originX;
			this.minY = this.maxY = originY;
			this.minZ = this.maxZ = originZ;
		}

		@Override
		public void accept(Ball ball) {
			if (this.ballIndex == this.balls.length) {
				this.balls = Arrays.copyOf(this.balls, this.ballIndex << 1);
			}
			double x = ball.x();
			double y = ball.y();
			double z = ball.z();
			double r = ball.radius();
			this.balls[this.ballIndex++] = (float)(x - this.originX);
			this.balls[this.ballIndex++] = (float)(y - this.originY);
			this.balls[this.ballIndex++] = (float)(z - this.originZ);
			this.balls[this.ballIndex++] = (float)(r);
			this.minX = Math.min(this.minX, x - r);
			this.minY = Math.min(this.minY, y - r);
			this.minZ = Math.min(this.minZ, z - r);
			this.maxX = Math.max(this.maxX, x + r);
			this.maxY = Math.max(this.maxY, y + r);
			this.maxZ = Math.max(this.maxZ, z + r);
		}

		public float[] getBalls() {
			return Arrays.copyOf(this.balls, this.ballIndex);
		}

		public BoundingBox getBox() {
			return new BoundingBox(
				ceilI(this.minX - 2.0D),
				ceilI(this.minY - 2.0D),
				ceilI(this.minZ - 2.0D),
				floorI(this.maxX + 2.0D),
				floorI(this.maxY + 2.0D),
				floorI(this.maxZ + 2.0D)
			);
		}
	}
}