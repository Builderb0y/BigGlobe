package builderb0y.bigglobe.scripting.wrappers;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.features.BlockQueueStructureWorldAccess;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.mixinInterfaces.ChunkOfColumnsHolder;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder.ColumnLookup;
import builderb0y.bigglobe.util.Rotation2D;
import builderb0y.bigglobe.util.Tripwire;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.bigglobe.versions.WorldVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper implements ColumnLookup {

	public static final TypeInfo TYPE = type(WorldWrapper.class);
	public static final MethodInfo GET_SEED = MethodInfo.getMethod(WorldWrapper.class, "getSeed").pure();

	public final StructureWorldAccess world;
	public final Coordination coordination;
	public final BlockPos.Mutable pos;
	public final Permuter permuter;
	public final WorldColumn randomColumn;
	/**
	sometimes, a Feature will get placed in a live world,
	completely outside of worldgen logic.
	in this case, {@link Tripwire} gets triggered a *lot*,
	because the chunks are usually not ProtoChunk's,
	and therefore not ChunkOfColumnsHolder's.
	we want to avoid log spam in this case,
	so we only check the chunk if this WorldWrapper
	is being used for worldgen purposes.
	*/
	public final boolean checkForColumns;
	public final boolean distantHorizons;

	public WorldWrapper(StructureWorldAccess world, Permuter permuter, Coordination coordination) {
		this.world = world;
		this.coordination = coordination;
		this.pos = new BlockPos.Mutable();
		this.permuter = permuter;
		this.randomColumn = WorldColumn.forWorld(world, 0, 0);
		while (world instanceof BlockQueueStructureWorldAccess queue) {
			world = queue.world;
		}
		this.checkForColumns = !(world instanceof World);
		this.distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
	}

	public @Nullable BlockPos.Mutable pos(int x, int y, int z) {
		return this.coordination.modifyPos(this.pos.set(x, y, z));
	}

	public BlockPos.Mutable unboundedPos(int x, int y, int z) {
		return this.coordination.modifyPosUnbounded(this.pos.set(x, y, z));
	}

	public long getSeed() {
		return this.world.getSeed();
	}

	@Override
	public WorldColumn lookupColumn(int x, int z) {
		BlockPos pos = this.unboundedPos(x, this.coordination.area.getMinY(), z);
		if (this.checkForColumns) {
			if (this.coordination.area.contains(pos)) {
				Chunk chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.EMPTY, false);
				if (chunk instanceof ChunkOfColumnsHolder holder) {
					ChunkOfColumns<? extends WorldColumn> columns = holder.bigglobe_getChunkOfColumns();
					if (columns != null) {
						WorldColumn column = columns.lookupColumn(pos.getX(), pos.getZ());
						if (column != null) {
							return column;
						}
						else if (Tripwire.isEnabled()) {
							Tripwire.logWithStackTrace("ChunkOfColumnsHolder at " + chunk.getPos() + " has the wrong coordinates? Requested " + pos.getX() + ", " + pos.getZ() + ", range covers from " + columns.getColumn(0).x + ", " + columns.getColumn(0).z + " to " + columns.getColumn(255).x + ", " + columns.getColumn(255).z);
						}
					}
					//distant horizons can sometimes create a new
					//Chunk object every time one is requested,
					//and of course that's not gonna have a ChunkOfColumns on it.
					//best not to log this case since it's a known issue.
					else if (Tripwire.isEnabled() && !this.distantHorizons) {
						Tripwire.logWithStackTrace("Chunk at " + chunk.getPos() + " is missing a ChunkOfColumns.");
					}
				}
				else if (Tripwire.isEnabled()) {
					Tripwire.logWithStackTrace("Chunk at [" + (pos.getX() >> 4) + ", " + (pos.getZ() >> 4) + " is not a ChunkOfColumnsHolder: " + chunk);
				}
			}
			else if (Tripwire.isEnabled()) {
				Tripwire.logWithStackTrace("Requested column " + pos.getX() + ", " + pos.getZ() + " outside bounds " + this.coordination.area);
			}
		}
		this.randomColumn.setPos(pos.getX(), pos.getZ());
		return this.randomColumn;
	}

	public BlockState getBlockState(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		return pos == null ? BlockStates.AIR : this.coordination.unmodifyState(this.world.getBlockState(pos));
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			state = this.coordination.modifyState(state);
			WorldUtil.setBlockState(this.world, pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			if (!state.getFluidState().isEmpty()) {
				WorldVersions.scheduleFluidTick(
					this.world,
					pos,
					state.getFluidState().getFluid(),
					state.getFluidState().getFluid().getTickRate(this.world)
				);
			}
		}
	}

	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.pos(x, y, z);
		return pos != null && SingleBlockFeature.place(this.world, pos, this.coordination.modifyState(state), SingleBlockFeature.IS_REPLACEABLE);
	}

	public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		BlockPos.Mutable pos = this.unboundedPos(minX, minY, minZ);
		minX = pos.getX(); minY = pos.getY(); minZ = pos.getZ();
		pos = this.unboundedPos(maxX, maxY, maxZ);
		maxX = pos.getX(); maxY = pos.getY(); maxZ = pos.getZ();
		int tmp;
		if (maxX < minX) { tmp = minX; minX = maxX; maxX = tmp; }
		if (maxY < minY) { tmp = minY; minY = maxY; maxY = tmp; }
		if (maxZ < minZ) { tmp = minZ; minZ = maxZ; maxZ = tmp; }
		minX = Math.max(minX, this.coordination.area.getMinX());
		minY = Math.max(minY, this.coordination.area.getMinY());
		minZ = Math.max(minZ, this.coordination.area.getMinZ());
		maxX = Math.min(maxX, this.coordination.area.getMaxX());
		maxY = Math.min(maxY, this.coordination.area.getMaxY());
		maxZ = Math.min(maxZ, this.coordination.area.getMaxZ());
		state = this.coordination.modifyState(state);
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					WorldUtil.setBlockState(this.world, pos.set(x, y, z), state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
				}
			}
		}
		if (!state.getFluidState().isEmpty()) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						WorldVersions.scheduleFluidTick(
							this.world,
							pos.set(x, y, z),
							state.getFluidState().getFluid(),
							state.getFluidState().getFluid().getTickRate(this.world)
						);
					}
				}
			}
		}
	}

	public boolean placeFeature(int x, int y, int z, ConfiguredFeatureEntry feature) {
		BlockPos pos = this.pos(x, y, z);
		return pos != null && feature.object().generate(
			this.world,
			((ServerChunkManager)(this.world.getChunkManager())).getChunkGenerator(),
			this.permuter.mojang(),
			pos
		);
	}

	public BiomeEntry getBiome(int x, int y, int z) {
		return new BiomeEntry(this.lookupColumn(x, z).getBiome(y));
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutOfHeightLimit(y);
	}

	public boolean isPositionValid(int x, int y, int z) {
		return this.isYLevelValid(y) && this.pos(x, y, z) != null;
	}

	public @Nullable NbtCompound getBlockData(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				return blockEntity.createNbtWithIdentifyingData();
			}
		}
		return null;
	}

	public void setBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				this.doSetBlockData(pos, blockEntity, nbt);
			}
		}
	}

	public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
				NbtCompound newData = oldData.copy().copyFrom(nbt);
				if (!oldData.equals(newData)) {
					this.doSetBlockData(pos, blockEntity, newData);
				}
			}
		}
	}

	public void doSetBlockData(BlockPos pos, BlockEntity blockEntity, NbtCompound nbt) {
		blockEntity.readNbt(nbt);
		blockEntity.markDirty();
		if (this.world instanceof World world) {
			BlockState state = this.world.getBlockState(pos);
			world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
		}
	}

	public void summon(String entityType, double x, double y, double z) {
		Vector3d newPos = this.coordination.modifyVec(new Vector3d(x, y, z));
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		Identifier identifier = new Identifier(entityType);
		if (RegistryVersions.entityType().containsId(identifier)) {
			Entity entity = RegistryVersions.entityType().get(identifier).create(this.world.toServerWorld());
			if (entity != null) {
				entity.refreshPositionAndAngles(newX, newY, newZ, entity.getYaw(), entity.getPitch());
				this.world.spawnEntityAndPassengers(entity);
			}
			else {
				throw new IllegalArgumentException("Entity type " + entityType + " is not enabled in this world's feature flags.");
			}
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityType);
		}
	}

	public void summon(String entityType, double x, double y, double z, NbtCompound nbt) {
		Vector3d newPos = this.coordination.modifyVec(new Vector3d(x, y, z));
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		NbtCompound copy = nbt.copy();
		copy.putString("id", entityType);
		Entity entity = EntityType.loadEntityWithPassengers(copy, this.world.toServerWorld(), entity1 -> {
			entity1.refreshPositionAndAngles(newX, newY, newZ, entity1.getYaw(), entity1.getPitch());
			return entity1;
		});
		if (entity != null) this.world.spawnEntityAndPassengers(entity);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}

	public static record Coordination(Rotation2D rotation, BlockBox area) {

		public static BlockPos.Mutable rotate(BlockPos.Mutable pos, Rotation2D rotation) {
			int x = rotation.getX(pos.getX(), pos.getZ());
			int z = rotation.getZ(pos.getX(), pos.getZ());
			return pos.setX(x).setZ(z);
		}

		public BlockPos.Mutable modifyPosUnbounded(BlockPos.Mutable pos) {
			return rotate(pos, this.rotation);
		}

		public BlockPos.@Nullable Mutable modifyPos(BlockPos.Mutable pos) {
			return this.area.contains(this.modifyPosUnbounded(pos)) ? pos : null;
		}

		public static Vector3d rotate(Vector3d vector, Rotation2D rotation) {
			double x = rotation.getX(vector.x - 0.5D, vector.z - 0.5D) + 0.5D;
			double z = rotation.getZ(vector.x - 0.5D, vector.z - 0.5D) + 0.5D;
			vector.x = x;
			vector.z = z;
			return vector;
		}

		public Vector3d modifyVecUnbounded(Vector3d vector) {
			return rotate(vector, this.rotation);
		}

		public @Nullable Vector3d modifyVec(Vector3d vector) {
			this.modifyVecUnbounded(vector);
			if (
				vector.x >= this.area.getMinX() && vector.x <= this.area.getMaxX() + 1 &&
				vector.y >= this.area.getMinY() && vector.y <= this.area.getMaxY() + 1 &&
				vector.z >= this.area.getMinZ() && vector.z <= this.area.getMaxZ() + 1
			) {
				return vector;
			}
			else {
				return null;
			}
		}

		public BlockState modifyState(BlockState state) {
			return state.rotate(this.rotation.rotation());
		}

		public BlockState unmodifyState(BlockState state) {
			return state.rotate(switch (this.rotation.rotation()) {
				case NONE -> BlockRotation.NONE;
				case CLOCKWISE_90 -> BlockRotation.COUNTERCLOCKWISE_90;
				case CLOCKWISE_180 -> BlockRotation.CLOCKWISE_180;
				case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
			});
		}
	}
}