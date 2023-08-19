package builderb0y.bigglobe.scripting.wrappers;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.ChunkOfColumns;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.mixinInterfaces.ChunkOfColumnsHolder;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder.ColumnLookup;
import builderb0y.bigglobe.util.Rotation2D;
import builderb0y.bigglobe.util.Tripwire;
import builderb0y.bigglobe.util.WorldOrChunk;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper implements ColumnLookup {

	public static final TypeInfo TYPE = type(WorldWrapper.class);
	public static final MethodInfo GET_SEED = MethodInfo.getMethod(WorldWrapper.class, "getSeed").pure();

	public final WorldOrChunk world;
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

	public WorldWrapper(WorldOrChunk world, Permuter permuter, Coordination coordination) {
		this.world = world;
		this.coordination = coordination;
		this.pos = new BlockPos.Mutable();
		this.permuter = permuter;
		this.randomColumn = world.createColumn(0, 0);
		this.checkForColumns = !world.isLive();
		this.distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
	}

	public BlockPos.Mutable unboundedPos(int x, int y, int z) {
		return this.coordination.modifyPosUnbounded(this.pos.set(x, y, z));
	}

	public BlockPos.@Nullable Mutable mutablePos(int x, int y, int z) {
		return this.coordination.filterPosMutable(this.unboundedPos(x, y, z));
	}

	public BlockPos.@Nullable Mutable immutablePos(int x, int y, int z) {
		return this.coordination.filterPosImmutable(this.unboundedPos(x, y, z));
	}

	public long getSeed() {
		return this.world.getSeed();
	}

	@Override
	public WorldColumn lookupColumn(int x, int z) {
		BlockPos pos = this.unboundedPos(x, this.coordination.immutableArea.getMinY(), z);
		if (this.checkForColumns) {
			if (this.coordination.immutableArea.contains(pos)) {
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
				Tripwire.logWithStackTrace("Requested column " + pos.getX() + ", " + pos.getZ() + " outside bounds " + this.coordination.immutableArea);
			}
		}
		this.randomColumn.setPos(pos.getX(), pos.getZ());
		return this.randomColumn;
	}

	public BlockState getBlockState(int x, int y, int z) {
		BlockPos pos = this.immutablePos(x, y, z);
		return pos == null ? BlockStates.AIR : this.coordination.unmodifyState(this.world.getBlockState(pos));
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		this.setBlockStateConditional(x, y, z, state, null);
	}

	public void setBlockStateReplaceable(int x, int y, int z, BlockState state) {
		this.setBlockStateConditional(x, y, z, state, SingleBlockFeature.IS_REPLACEABLE);
	}

	public void setBlockStateNonReplaceable(int x, int y, int z, BlockState state) {
		this.setBlockStateConditional(x, y, z, state, SingleBlockFeature.NOT_REPLACEABLE);
	}

	public void setBlockStateConditional(int x, int y, int z, BlockState state, Predicate<BlockState> predicate) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null && (predicate == null || predicate.test(this.world.getBlockState(pos)))) {
			state = this.coordination.modifyState(state);
			this.world.setBlockState(pos, state);
			if (!state.getFluidState().isEmpty()) {
				this.world.scheduleFluidTick(pos, state.getFluidState());
			}
		}
	}

	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.mutablePos(x, y, z);
		return pos != null && this.world.placeBlockState(pos, this.coordination.modifyState(state));
	}

	public void fillBlockState(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		this.fillBlockStateConditionally(minX, minY, minZ, maxX, maxY, maxZ, state, null);
	}

	public void fillBlockStateReplaceable(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		this.fillBlockStateConditionally(minX, minY, minZ, maxX, maxY, maxZ, state, SingleBlockFeature.IS_REPLACEABLE);
	}

	public void fillBlockStateNonReplaceable(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		this.fillBlockStateConditionally(minX, minY, minZ, maxX, maxY, maxZ, state, SingleBlockFeature.NOT_REPLACEABLE);
	}

	public void fillBlockStateConditionally(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, Predicate<BlockState> predicate) {
		BlockPos.Mutable pos = this.unboundedPos(minX, minY, minZ);
		minX = pos.getX(); minY = pos.getY(); minZ = pos.getZ();
		pos = this.unboundedPos(maxX, maxY, maxZ);
		maxX = pos.getX(); maxY = pos.getY(); maxZ = pos.getZ();
		int tmp;
		if (maxX < minX) { tmp = minX; minX = maxX; maxX = tmp; }
		if (maxY < minY) { tmp = minY; minY = maxY; maxY = tmp; }
		if (maxZ < minZ) { tmp = minZ; minZ = maxZ; maxZ = tmp; }
		minX = Math.max(minX, this.coordination.mutableArea.getMinX());
		minY = Math.max(minY, this.coordination.mutableArea.getMinY());
		minZ = Math.max(minZ, this.coordination.mutableArea.getMinZ());
		maxX = Math.min(maxX, this.coordination.mutableArea.getMaxX());
		maxY = Math.min(maxY, this.coordination.mutableArea.getMaxY());
		maxZ = Math.min(maxZ, this.coordination.mutableArea.getMaxZ());
		state = this.coordination.modifyState(state);
		for (int z = minZ; z <= maxZ; z++) {
			pos.setZ(z);
			for (int x = minX; x <= maxX; x++) {
				pos.setX(x);
				for (int y = minY; y <= maxY; y++) {
					pos.setY(y);
					if (predicate == null || predicate.test(this.world.getBlockState(pos))) {
						this.world.setBlockState(pos, state);
						if (!state.getFluidState().isEmpty()) {
							this.world.scheduleFluidTick(pos, state.getFluidState());
						}
					}
				}
			}
		}
	}

	public boolean placeFeature(int x, int y, int z, ConfiguredFeatureEntry feature) {
		BlockPos pos = this.mutablePos(x, y, z);
		return pos != null && this.world.placeFeature(pos, feature.object(), this.permuter.mojang());
	}

	public StructurePlacementData newStructurePlacementData() {
		return new StructurePlacementData().setBoundingBox(this.coordination.mutableArea);
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template) {
		this.world.placeStructureTemplate(x, y, z, template, new StructurePlacementData(), this.permuter);
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlacementData data) {
		this.world.placeStructureTemplate(x, y, z, template, data, this.permuter);
	}

	public BiomeEntry getBiome(int x, int y, int z) {
		return new BiomeEntry(this.lookupColumn(x, z).getBiome(y));
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutOfHeightLimit(y);
	}

	public boolean isPositionValid(int x, int y, int z) {
		return this.isYLevelValid(y) && this.mutablePos(x, y, z) != null;
	}

	public @Nullable NbtCompound getBlockData(int x, int y, int z) {
		BlockPos pos = this.immutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				return blockEntity.createNbtWithIdentifyingData();
			}
		}
		return null;
	}

	public void setBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				blockEntity.readNbt(nbt);
				blockEntity.markDirty();
			}
		}
	}

	public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
				NbtCompound newData = oldData.copy().copyFrom(nbt);
				if (!oldData.equals(newData)) {
					blockEntity.readNbt(newData);
					blockEntity.markDirty();
				}
			}
		}
	}

	public void summon(String entityType, double x, double y, double z) {
		Vector3d newPos = this.coordination.filterVecMutable(
			this.coordination.modifyVecUnbounded(
				new Vector3d(x, y, z)
			)
		);
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		Identifier identifier = new Identifier(entityType);
		if (RegistryVersions.entityType().containsId(identifier)) {
			this.world.spawnEntity(serverWorld -> {
				Entity entity = RegistryVersions.entityType().get(identifier).create(serverWorld);
				if (entity != null) {
					entity.refreshPositionAndAngles(newX, newY, newZ, entity.getYaw(), entity.getPitch());
					return entity;
				}
				else {
					throw new IllegalArgumentException("Entity type " + entityType + " is not enabled in this world's feature flags.");
				}
			});
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityType);
		}
	}

	public void summon(String entityType, double x, double y, double z, NbtCompound nbt) {
		Vector3d newPos = this.coordination.filterVecMutable(
			this.coordination.modifyVecUnbounded(
				new Vector3d(x, y, z)
			)
		);
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		NbtCompound copy = nbt.copy();
		copy.putString("id", entityType);
		this.world.spawnEntity(serverWorld -> {
			return EntityType.loadEntityWithPassengers(copy, serverWorld, entity -> {
				entity.refreshPositionAndAngles(newX, newY, newZ, entity.getYaw(), entity.getPitch());
				return entity;
			});
		});
	}

	public Coordinator coordinator() {
		return (
			this
			.world
			.coordinator()
			.inBox(this.coordination.mutableArea, false)
			.translate(
				this.coordination.rotation.offsetX(),
				this.coordination.rotation.offsetY(),
				this.coordination.rotation.offsetZ()
			)
			.rotate1x(this.coordination.rotation.rotation())
		);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}

	public static record Coordination(Rotation2D rotation, BlockBox mutableArea, BlockBox immutableArea) {

		public static BlockPos.Mutable rotate(BlockPos.Mutable pos, Rotation2D rotation) {
			int x = rotation.getX(pos.getX(), pos.getY(), pos.getZ());
			int y = rotation.getY(pos.getX(), pos.getY(), pos.getZ());
			int z = rotation.getZ(pos.getX(), pos.getY(), pos.getZ());
			return pos.set(x, y, z);
		}

		public BlockPos.Mutable modifyPosUnbounded(BlockPos.Mutable pos) {
			return rotate(pos, this.rotation);
		}

		public BlockPos.@Nullable Mutable filterPosMutable(BlockPos.Mutable pos) {
			return this.mutableArea.contains(pos) ? pos : null;
		}

		public BlockPos.@Nullable Mutable filterPosImmutable(BlockPos.Mutable pos) {
			return this.immutableArea.contains(pos) ? pos : null;
		}

		public static Vector3d rotate(Vector3d vector, Rotation2D rotation) {
			double x = vector.x - 0.5D;
			double y = vector.y;
			double z = vector.z - 0.5D;
			vector.x = rotation.getX(x, y, z) + 0.5D;
			vector.y = rotation.getY(x, y, z);
			vector.z = rotation.getZ(x, y, z) + 0.5D;
			return vector;
		}

		public Vector3d modifyVecUnbounded(Vector3d vector) {
			return rotate(vector, this.rotation);
		}

		public static boolean contains(BlockBox area, double x, double y, double z) {
			return (
				x >= area.getMinX() && x <= area.getMaxX() + 1 &&
				y >= area.getMinY() && y <= area.getMaxY() + 1 &&
				z >= area.getMinZ() && z <= area.getMaxZ() + 1
			);
		}

		public @Nullable Vector3d filterVecMutable(Vector3d vector) {
			return contains(this.mutableArea, vector.x, vector.y, vector.z) ? vector : null;
		}

		public @Nullable Vector3d filterVecImmutable(Vector3d vector) {
			return contains(this.immutableArea, vector.x, vector.y, vector.z) ? vector : null;
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