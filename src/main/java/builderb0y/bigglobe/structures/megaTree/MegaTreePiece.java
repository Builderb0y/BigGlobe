package builderb0y.bigglobe.structures.megaTree;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.joml.Vector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
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
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.dynamicRegistries.WoodPalette;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.DataStructurePiece;
import builderb0y.bigglobe.util.Vectors;
import builderb0y.bigglobe.versions.BlockStateVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

import static builderb0y.bigglobe.math.BigGlobeMath.*;

public class MegaTreePiece extends DataStructurePiece<MegaTreePiece.Data> {

	public static record Data(
		RegistryEntry<Structure> structure,
		@Hidden MegaTreeStructure actualStructure,
		RegistryEntry<WoodPalette> wood,
		double originX,
		double originY,
		double originZ,
		float[] balls
	) {
		public static final AutoCoder<Data> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(Data.class);

		public Data(RegistryEntry<Structure> structure, RegistryEntry<WoodPalette> wood, double originX, double originY, double originZ, float[] offsetsAndRadii) {
			this(structure, ((MegaTreeStructure)(structure.value())), wood, originX, originY, originZ, offsetsAndRadii);
		}

		public Data(MegaTreeStructure actualStructure, RegistryEntry<WoodPalette> wood, double originX, double originY, double originZ, float[] offsetsAndRadii) {
			this(getActualEntry(actualStructure), actualStructure, wood, originX, originY, originZ, offsetsAndRadii);
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

		public static RegistryEntry<Structure> getActualEntry(MegaTreeStructure structure) {
			return RegistryVersions.getEntry(
				RegistryVersions.getRegistry(
					BigGlobeMod.getCurrentServer().getRegistryManager(),
					RegistryKeys.STRUCTURE
				),
				structure
			);
		}
	}

	public MegaTreePiece(
		StructurePieceType type,
		MegaTreeStructure structure,
		RegistryEntry<WoodPalette> wood,
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

	public MegaTreePiece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
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
		ScriptedColumn column = snowChance != null && chunkGenerator instanceof BigGlobeScriptedChunkGenerator scriptedGenerator ? scriptedGenerator.newColumn(world, 0, 0, ColumnUsage.GENERIC.maybeDhHints()) : null;
		Permuter permuter = new Permuter(Permuter.permute(world.getSeed() ^ 0xD6B4371F4701555BL, chunkBox.getMinX() >> 4, chunkBox.getMinY() >> 4, chunkBox.getMinZ() >> 4));
		WoodPalette wood = this.data.wood.value();
		Vector3d unitGenerator = new Vector3d();

		for (int index = 0, count = this.data.countBalls(); index < count; index++) {
			double
				centerX = this.data.getX(index),
				centerY = this.data.getY(index),
				centerZ = this.data.getZ(index),
				radius  = this.data.getRadius(index),
				radiusSquared = radius * radius;
			int
				minX = Math.max( ceilI(centerX - radius), chunkBox.getMinX()),
				minZ = Math.max( ceilI(centerZ - radius), chunkBox.getMinZ()),
				maxX = Math.min(floorI(centerX + radius), chunkBox.getMaxX()),
				maxZ = Math.min(floorI(centerZ + radius), chunkBox.getMaxZ());
			boolean
				placedAnyLogs = false;
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
								world.setBlockState(pos, wood.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
								placedAnyLogs = true;
							}
							else {
								break;
							}
						}
						for (pos.setY(minY); pos.getY() <= maxY; pos.setY(pos.getY() + 1)) {
							if (this.canLogReplace(world.getBlockState(pos))) {
								world.setBlockState(pos, wood.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
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
					world.setBlockState(pos, wood.woodState(permuter, Axis.Y), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
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
							pos.getX() >= chunkBox.getMinX() &&
							pos.getX() <= chunkBox.getMaxX() &&
							pos.getZ() >= chunkBox.getMinZ() &&
							pos.getZ() <= chunkBox.getMaxZ()
						) {
							int topY = pos.getY();
							for (
								int bits = permuter.nextInt() | 1;
								(bits & 1) != 0 && this.canLeavesReplace(world.getBlockState(pos));
								bits >>>= 1, pos.setY(pos.getY() - 1)
							) {
								world.setBlockState(pos, wood.leavesState(permuter, 7, true, false), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							}
							this.placeSnow(world, pos.setY(topY + 1), column, snowChance, permuter);
						}
					}
				}
			}
		}
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

	public static class BallCollection implements Consumer<MegaTreeBall> {

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
		public void accept(MegaTreeBall ball) {
			if (this.ballIndex == this.balls.length) {
				this.balls = Arrays.copyOf(this.balls, this.ballIndex << 1);
			}
			double x = ball.data.x();
			double y = ball.data.y();
			double z = ball.data.z();
			double r = ball.data.radius();
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

		public BlockBox getBox() {
			return new BlockBox(
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