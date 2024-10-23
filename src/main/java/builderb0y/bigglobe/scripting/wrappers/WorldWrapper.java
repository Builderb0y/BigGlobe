package builderb0y.bigglobe.scripting.wrappers;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.overriders.ColumnValueOverrider;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldOrChunk;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.BoundInfoHolder;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper implements ScriptedColumnLookup {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo random;
		public MethodInfo seed, minValidYLevel, maxValidYLevel, hints, distantHorizons, surfaceOnly;

		public InsnTree seed(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.seed);
		}

		public InsnTree minValidYLevel(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.minValidYLevel);
		}

		public InsnTree maxValidYLevel(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.maxValidYLevel);
		}

		public InsnTree random(InsnTree loadWorld) {
			return getField(loadWorld, this.random);
		}

		public InsnTree hints(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.hints);
		}

		public InsnTree distantHorizons(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.distantHorizons);
		}

		public InsnTree surfaceOnly(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.surfaceOnly);
		}
	}

	public static final BoundInfo BOUND_PARAM = new BoundInfo(load("world", INFO.type));
	public static class BoundInfo extends BoundInfoHolder {

		public InsnTree random, seed, hints, distantHorizons;

		public BoundInfo(InsnTree loadWorld) {
			super(INFO, loadWorld);
		}
	}

	public final WorldOrChunk world;
	public final Coordination coordination;
	public final BlockPos.Mutable pos;
	public final RandomGenerator random;
	public final ScriptedColumn.Factory columnFactory;
	public final Long2ObjectOpenHashMap<ScriptedColumn> columns;
	public final ScriptedColumn.Params params;
	public AutoOverride overriders;

	public WorldWrapper(
		WorldOrChunk world,
		BigGlobeScriptedChunkGenerator chunkGenerator,
		RandomGenerator random,
		Coordination coordination,
		Hints hints
	) {
		this.world = world;
		this.coordination = coordination;
		this.pos = new BlockPos.Mutable();
		this.random = random;
		this.columnFactory = chunkGenerator.columnEntryRegistry.columnFactory;
		this.params = new ScriptedColumn.Params(
			chunkGenerator.columnSeed,
			0,
			0,
			coordination.mutableArea.getMinY(),
			coordination.mutableArea.getMaxY(),
			hints,
			chunkGenerator.compiledWorldTraits
		);
		if (world instanceof ChunkDelegator delegator) {
			delegator.worldWrapper = this;
		}
		if (ScriptedColumnLookup.GLOBAL.getCurrent() instanceof WorldWrapper parent) {
			this.columns = parent.columns;
			this.overriders = parent.overriders;
		}
		else {
			this.columns = new Long2ObjectOpenHashMap<>(64);
		}
	}

	public static record AutoOverride(ScriptStructures structures, ColumnValueOverrider.Holder[] overriders, String[] preFetch) {

		public void override(ScriptedColumn column) {
			for (String name : this.preFetch) try {
				column.preComputeColumnValue(name);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception pre-computing column value for overrider: ", throwable);
			}
			for (ColumnValueOverrider.Holder overrider : this.overriders) {
				overrider.override(column, this.structures);
			}
		}
	}

	@Override
	public ScriptedColumn lookupColumn(int x, int z) {
		return this.columns.computeIfAbsent(ColumnPos.pack(x, z), (long packedPos) -> {
			ScriptedColumn column = this.columnFactory.create(
				this.params.at(ColumnPos.getX(packedPos), ColumnPos.getZ(packedPos))
			);
			if (this.overriders != null) {
				this.overriders.override(column);
			}
			return column;
		});
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

	public long seed() {
		return this.world.getSeed();
	}

	public Hints hints() {
		return this.params.hints();
	}

	public boolean distantHorizons() {
		return this.params.hints().isLod();
	}

	public boolean surfaceOnly() {
		return !this.params.hints().fill();
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
		if (pos != null) {
			Permuter permuter = new Permuter(Permuter.permute(this.seed() ^ 0xB5ECAC279BD1E7FBL, UnregisteredObjectException.getID(feature.entry()).hashCode(), x, y, z));
			return this.world.placeFeature(pos, feature.object(), permuter.mojang());
		}
		return false;
	}

	public StructurePlacementData newStructurePlacementData() {
		return new StructurePlacementData().setBoundingBox(this.coordination.mutableArea);
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template) {
		this.placeStructureTemplate(x, y, z, template, new StructurePlacementData());
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlacementData data) {
		data = data.copy();
		BlockPos pos = this.unboundedPos(x, y, z);
		x = pos.getX(); y = pos.getY(); z = pos.getZ();
		pos = this.unboundedPos(data.getPosition().getX(), data.getPosition().getY(), data.getPosition().getZ());
		data.setPosition(pos.toImmutable());
		Symmetry oldSymmetry = Symmetry.of(data.getMirror()).andThen(Symmetry.of(data.getRotation()));
		Symmetry newSymmetry = this.coordination.transformation().symmetry().andThen(oldSymmetry);
		data.setMirror(newSymmetry.isFlipped() ? BlockMirror.FRONT_BACK : BlockMirror.NONE);
		data.setRotation(switch (newSymmetry) {
			case IDENTITY, FLIP_0 -> BlockRotation.NONE;
			case ROTATE_90, FLIP_135 -> BlockRotation.CLOCKWISE_90;
			case ROTATE_180, FLIP_90 -> BlockRotation.CLOCKWISE_180;
			case ROTATE_270, FLIP_45 -> BlockRotation.COUNTERCLOCKWISE_90;
		});
		Permuter permuter = new Permuter(Permuter.permute(this.seed() ^ 0xD6ABF6E7480FDDE0L, x, y, z));
		this.world.placeStructureTemplate(x, y, z, template, data, permuter);
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutOfHeightLimit(y);
	}

	public boolean isPositionValid(int x, int y, int z) {
		return this.isYLevelValid(y) && this.mutablePos(x, y, z) != null;
	}

	public int minValidYLevel() {
		return this.world.getBottomY();
	}

	public int maxValidYLevel() {
		return this.world.getTopY();
	}

	public @Nullable NbtCompound getBlockData(int x, int y, int z) {
		BlockPos pos = this.immutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				return BlockEntityVersions.writeToNbt(blockEntity);
			}
		}
		return null;
	}

	public void setBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				BlockEntityVersions.readFromNbt(blockEntity, nbt);
				blockEntity.markDirty();
			}
		}
	}

	public void mergeBlockData(int x, int y, int z, NbtCompound nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				NbtCompound oldData = BlockEntityVersions.writeToNbt(blockEntity);
				NbtCompound newData = oldData.copy().copyFrom(nbt);
				if (!oldData.equals(newData)) {
					BlockEntityVersions.readFromNbt(blockEntity, newData);
					blockEntity.markDirty();
				}
			}
		}
	}

	public void summon(double x, double y, double z, String entityType) {
		Vector3d newPos = this.coordination.filterVecMutable(
			this.coordination.modifyVecUnbounded(
				new Vector3d(x, y, z)
			)
		);
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		Identifier identifier = IdentifierVersions.create(entityType);
		if (RegistryVersions.entityType().containsId(identifier)) {
			this.world.spawnEntity((ServerWorld serverWorld) -> {
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

	public void summon(double x, double y, double z, String entityType, NbtCompound nbt) {
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
		this.world.spawnEntity((ServerWorld serverWorld) -> {
			return EntityType.loadEntityWithPassengers(copy, serverWorld, (Entity entity) -> {
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
				this.coordination.transformation.offsetX(),
				this.coordination.transformation.offsetY(),
				this.coordination.transformation.offsetZ()
			)
			.symmetric(this.coordination.transformation.symmetry())
		);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}

	public static record Coordination(SymmetricOffset transformation, BlockBox mutableArea, BlockBox immutableArea) {

		public static BlockPos.Mutable rotate(BlockPos.Mutable pos, SymmetricOffset rotation) {
			int x = rotation.getX(pos.getX(), pos.getY(), pos.getZ());
			int y = rotation.getY(pos.getX(), pos.getY(), pos.getZ());
			int z = rotation.getZ(pos.getX(), pos.getY(), pos.getZ());
			return pos.set(x, y, z);
		}

		public BlockPos.Mutable modifyPosUnbounded(BlockPos.Mutable pos) {
			return rotate(pos, this.transformation);
		}

		public BlockPos.@Nullable Mutable filterPosMutable(BlockPos.Mutable pos) {
			return this.mutableArea.contains(pos) ? pos : null;
		}

		public BlockPos.@Nullable Mutable filterPosImmutable(BlockPos.Mutable pos) {
			return this.immutableArea.contains(pos) ? pos : null;
		}

		public static Vector3d rotate(Vector3d vector, SymmetricOffset rotation) {
			double x = vector.x - 0.5D;
			double y = vector.y;
			double z = vector.z - 0.5D;
			vector.x = rotation.getX(x, y, z) + 0.5D;
			vector.y = rotation.getY(x, y, z);
			vector.z = rotation.getZ(x, y, z) + 0.5D;
			return vector;
		}

		public Vector3d modifyVecUnbounded(Vector3d vector) {
			return rotate(vector, this.transformation);
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
			return this.transformation.symmetry().apply(state);
		}

		public BlockState unmodifyState(BlockState state) {
			return this.transformation.symmetry().inverse().apply(state);
		}
	}
}