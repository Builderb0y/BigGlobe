package builderb0y.bigglobe.scripting.wrappers;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnValueInfo;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.WorldInfo;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.overriders.ColumnValueOverrider;
import builderb0y.bigglobe.scripting.wrappers.entries.ConfiguredFeatureEntry;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldOrChunk;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.BoundInfoHolder;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper implements ScriptedColumnLookup {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public FieldInfo
			random;
		public MethodInfo
			seed,
			minValidYLevel,
			maxValidYLevel,
			hints,
			originX,
			originY,
			originZ;

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

		public InsnTree originX(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.originX);
		}

		public InsnTree originY(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.originY);
		}

		public InsnTree originZ(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.originZ);
		}
	}

	public static final BoundInfo BOUND_PARAM = new BoundInfo(load("world", INFO.type));

	public static class BoundInfo extends BoundInfoHolder {

		public InsnTree
			random,
			seed,
			hints,
			originX,
			originY,
			originZ;

		public BoundInfo(InsnTree loadWorld) {
			super(INFO, loadWorld);
		}
	}

	public final WorldOrChunk world;
	public final Coordination coordination;
	public final MutableBlockPos pos;
	public Vector3d doublePos;
	public final RandomGenerator random;
	public long featureSalt = 0xB5ECAC279BD1E7FBL;
	public final ConfiguredColumnFactory columnFactory;
	public final Long2ObjectOpenHashMap<ScriptedColumn> columns;
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
		this.pos = new MutableBlockPos();
		this.random = random;
		if (world instanceof ChunkDelegator delegator) {
			delegator.worldWrapper = this;
		}
		if (ScriptedColumnLookup.GLOBAL.currentValue() instanceof WorldWrapper parent) {
			this.columns = parent.columns;
			this.overriders = parent.overriders;
			this.columnFactory = parent.columnFactory;
		}
		else {
			this.columns = new Long2ObjectOpenHashMap<>(64);
			this.columnFactory = new ConfiguredColumnFactory(
				chunkGenerator.columnEntryRegistry.columnFactory,
				new WorldInfo(
					chunkGenerator.columnSeed,
					coordination.mutableArea.minY(),
					coordination.mutableArea.maxY() + 1,
					chunkGenerator.compiledWorldTraits
				),
				hints
			);
		}
	}

	public WorldWrapper(WorldWrapper from, Coordination coordination) {
		this.world = from.world;
		this.coordination = coordination;
		this.pos = new MutableBlockPos();
		this.random = new Permuter(from.random.nextLong());
		this.columnFactory = from.columnFactory;
		this.columns = from.columns;
		this.overriders = from.overriders;
	}

	public static record AutoOverride(ScriptStructures[] structures, Holder<ColumnValueOverrider.Entry>[] overriders, ColumnValueInfo[] preFetch) {

		public AutoOverride {
			if (structures.length != overriders.length) {
				throw new IllegalArgumentException("Wrong length!");
			}
		}

		public void override(ScriptedColumn column) {
			for (ColumnValueInfo info : this.preFetch) try {
				info.preComputer().invokeExact(column);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception pre-computing column value for overrider: ", throwable);
			}
			for (int index = 0; index < this.structures.length; index++) {
				this.overriders[index].value().script.override(column, this.structures[index]);
			}
		}
	}

	@Override
	public ConfiguredColumnFactory getSource() {
		return this.columnFactory;
	}

	@Override
	public ScriptedColumn lookupColumn(int x, int z) {
		BlockPos pos = this.unboundedPos(x, 0, z);
		x = pos.getX();
		z = pos.getZ();
		return this.columns.computeIfAbsent(
			ColumnPos.asLong(x, z), (long packedPos) -> {
				ScriptedColumn column = this.columnFactory.createAt(
					ColumnPos.getX(packedPos),
					ColumnPos.getZ(packedPos)
				);
				if (this.overriders != null) {
					this.overriders.override(column);
				}
				return column;
			}
		);
	}

	public int originX() {
		return this.coordination.transformation.offsetX();
	}

	public int originY() {
		return this.coordination.transformation.offsetY();
	}

	public int originZ() {
		return this.coordination.transformation.offsetZ();
	}

	public MutableBlockPos unboundedPos(int x, int y, int z) {
		return this.coordination.modifyPosUnbounded(this.pos.set(x, y, z));
	}

	public @Nullable MutableBlockPos mutablePos(int x, int y, int z) {
		return this.coordination.filterPosMutable(this.unboundedPos(x, y, z));
	}

	public @Nullable MutableBlockPos immutablePos(int x, int y, int z) {
		return this.coordination.filterPosImmutable(this.unboundedPos(x, y, z));
	}

	public int transformX(int x, int y, int z) {
		return this.unboundedPos(x, y, z).getX();
	}

	public int transformY(int x, int y, int z) {
		return this.unboundedPos(x, y, z).getY();
	}

	public int transformZ(int x, int y, int z) {
		return this.unboundedPos(x, y, z).getZ();
	}

	public Vector3d transform(double x, double y, double z) {
		return this.coordination.modifyVecUnbounded(this.doublePos(x, y, z));
	}

	public double transformX(double x, double y, double z) {
		return this.transform(x, y, z).x;
	}

	public double transformY(double x, double y, double z) {
		return this.transform(x, y, z).y;
	}

	public double transformZ(double x, double y, double z) {
		return this.transform(x, y, z).z;
	}

	public Vector3d doublePos(double x, double y, double z) {
		Vector3d pos = this.doublePos;
		if (pos == null) pos = this.doublePos = new Vector3d();
		return pos.set(x, y, z);
	}

	public long seed() {
		return this.world.getSeed();
	}

	public Hints hints() {
		return this.columnFactory.hints();
	}

	public BlockState getBlockState(int x, int y, int z) {
		if (this.isInCrashRange(y)) {
			throw new IllegalArgumentException("The provided Y coordinate is very far outside the world height limit. The script may be searching for blocks in a column that has none.");
		}
		BlockPos pos = this.immutablePos(x, y, z);
		return pos == null ? BlockStates.AIR : this.coordination.unmodifyState(this.world.getBlockState(pos));
	}

	public boolean setBlockState(int x, int y, int z, BlockState state) {
		return this.setBlockStateConditional(x, y, z, state, null);
	}

	public boolean setBlockStateReplaceable(int x, int y, int z, BlockState state) {
		return this.setBlockStateConditional(x, y, z, state, SingleBlockFeature.IS_REPLACEABLE);
	}

	public boolean setBlockStateNonReplaceable(int x, int y, int z, BlockState state) {
		return this.setBlockStateConditional(x, y, z, state, SingleBlockFeature.NOT_REPLACEABLE);
	}

	public boolean setBlockStateConditional(int x, int y, int z, BlockState state, Predicate<BlockState> predicate) {
		if (state != null) {
			BlockPos pos = this.mutablePos(x, y, z);
			if (pos != null && (predicate == null || predicate.test(this.world.getBlockState(pos)))) {
				state = this.coordination.modifyState(state);
				this.world.setBlockState(pos, state);
				if (!state.getFluidState().isEmpty()) {
					this.world.scheduleFluidTick(pos, state.getFluidState());
				}
				return true;
			}
		}
		return false;
	}

	public boolean placeBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.mutablePos(x, y, z);
		return pos != null && this.world.placeBlockState(pos, this.coordination.modifyState(state));
	}

	public boolean updateBlockState(int x, int y, int z) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			this.world.updateBlockState(pos);
			return true;
		}
		return false;
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
		if (state != null) {
			MutableBlockPos pos = this.unboundedPos(minX, minY, minZ);
			minX = pos.getX();
			minY = pos.getY();
			minZ = pos.getZ();
			pos = this.unboundedPos(maxX, maxY, maxZ);
			maxX = pos.getX();
			maxY = pos.getY();
			maxZ = pos.getZ();
			int tmp;
			if (maxX < minX) {
				tmp = minX;
				minX = maxX;
				maxX = tmp;
			}
			if (maxY < minY) {
				tmp = minY;
				minY = maxY;
				maxY = tmp;
			}
			if (maxZ < minZ) {
				tmp = minZ;
				minZ = maxZ;
				maxZ = tmp;
			}
			minX = Math.max(minX, this.coordination.mutableArea.minX());
			minY = Math.max(minY, this.coordination.mutableArea.minY());
			minZ = Math.max(minZ, this.coordination.mutableArea.minZ());
			maxX = Math.min(maxX, this.coordination.mutableArea.maxX());
			maxY = Math.min(maxY, this.coordination.mutableArea.maxY());
			maxZ = Math.min(maxZ, this.coordination.mutableArea.maxZ());
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
	}

	public void updateBlockStates(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		MutableBlockPos pos = this.unboundedPos(minX, minY, minZ);
		minX = pos.getX();
		minY = pos.getY();
		minZ = pos.getZ();
		pos = this.unboundedPos(maxX, maxY, maxZ);
		maxX = pos.getX();
		maxY = pos.getY();
		maxZ = pos.getZ();
		int tmp;
		if (maxX < minX) {
			tmp = minX;
			minX = maxX;
			maxX = tmp;
		}
		if (maxY < minY) {
			tmp = minY;
			minY = maxY;
			maxY = tmp;
		}
		if (maxZ < minZ) {
			tmp = minZ;
			minZ = maxZ;
			maxZ = tmp;
		}
		minX = Math.max(minX, this.coordination.mutableArea.minX());
		minY = Math.max(minY, this.coordination.mutableArea.minY());
		minZ = Math.max(minZ, this.coordination.mutableArea.minZ());
		maxX = Math.min(maxX, this.coordination.mutableArea.maxX());
		maxY = Math.min(maxY, this.coordination.mutableArea.maxY());
		maxZ = Math.min(maxZ, this.coordination.mutableArea.maxZ());
		for (int z = minZ; z <= maxZ; z++) {
			pos.setZ(z);
			for (int x = minX; x <= maxX; x++) {
				pos.setX(x);
				for (int y = minY; y <= maxY; y++) {
					this.world.updateBlockState(pos.setY(y));
				}
			}
		}
	}

	public boolean placeFeature(int x, int y, int z, ConfiguredFeatureEntry feature) {
		if (feature != null) {
			BlockPos pos = this.mutablePos(x, y, z);
			if (pos != null) {
				Permuter permuter = new Permuter(Permuter.permute(this.seed() ^ this.featureSalt, feature.identifier().hashCode(), pos.getX(), pos.getY(), pos.getZ()));
				return this.world.placeFeature(pos, feature.object(), permuter.mojang());
			}
		}
		return false;
	}

	public StructurePlaceSettings newStructurePlacementData() {
		return new StructurePlaceSettings().setBoundingBox(this.coordination.mutableArea);
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template) {
		this.placeStructureTemplate(x, y, z, template, this.newStructurePlacementData());
	}

	public void placeStructureTemplate(int x, int y, int z, StructureTemplate template, StructurePlaceSettings data) {
		data = data.copy();
		BlockPos pos = this.unboundedPos(x, y, z);
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
		Symmetry oldSymmetry = Symmetry.of(data.getMirror()).andThen(Symmetry.of(data.getRotation()));
		Symmetry newSymmetry = this.coordination.transformation().symmetry().andThen(oldSymmetry);
		data.setMirror(newSymmetry.isFlipped() ? Mirror.FRONT_BACK : Mirror.NONE);
		data.setRotation(switch (newSymmetry) {
			case IDENTITY, FLIP_0 -> Rotation.NONE;
			case ROTATE_90, FLIP_135 -> Rotation.CLOCKWISE_90;
			case ROTATE_180, FLIP_90 -> Rotation.CLOCKWISE_180;
			case ROTATE_270, FLIP_45 -> Rotation.COUNTERCLOCKWISE_90;
		});
		Permuter permuter = new Permuter(Permuter.permute(this.seed() ^ 0xD6ABF6E7480FDDE0L, x, y, z));
		this.world.placeStructureTemplate(x, y, z, template, data, permuter);
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutsideBuildHeight(y + this.coordination.transformation.offsetY());
	}

	public boolean isInCrashRange(int y) {
		y += this.coordination.transformation.offsetY();
		return y < HeightLimitViewVersions.getMinY(this.world) - 64 || y >= HeightLimitViewVersions.getMaxY(this.world) + 64;
	}

	public boolean isPositionValid(int x, int y, int z) {
		return this.isYLevelValid(y) && this.mutablePos(x, y, z) != null;
	}

	public int minValidYLevel() {
		return this.world.getMinY();
	}

	public int maxValidYLevel() {
		return HeightLimitViewVersions.getMaxY(this.world);
	}

	public @Nullable CompoundTag getBlockData(int x, int y, int z) {
		BlockPos pos = this.immutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				return BlockEntityVersions.writeToNbt(blockEntity);
			}
		}
		return null;
	}

	public void setBlockData(int x, int y, int z, CompoundTag nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				BlockEntityVersions.readFromNbt(blockEntity, nbt);
				blockEntity.setChanged();
			}
		}
	}

	public void mergeBlockData(int x, int y, int z, CompoundTag nbt) {
		BlockPos pos = this.mutablePos(x, y, z);
		if (pos != null) {
			BlockEntity blockEntity = this.world.getBlockEntity(pos);
			if (blockEntity != null) {
				CompoundTag oldData = BlockEntityVersions.writeToNbt(blockEntity);
				CompoundTag newData = oldData.copy().merge(nbt);
				if (!oldData.equals(newData)) {
					BlockEntityVersions.readFromNbt(blockEntity, newData);
					blockEntity.setChanged();
				}
			}
		}
	}

	public void summon(double x, double y, double z, String entityType) {
		Vector3d newPos = this.coordination.filterVecMutable(
			this.transform(x, y, z)
		);
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		Identifier identifier = IdentifierVersions.create(entityType);
		if (BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
			this.world.spawnEntity((ServerLevel serverWorld) -> {
				Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(identifier).create(serverWorld, EntitySpawnReason.CHUNK_GENERATION);
				if (entity != null) {
					entity.snapTo(newX, newY, newZ, entity.getYRot(), entity.getXRot());
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

	public void summon(double x, double y, double z, String entityType, CompoundTag nbt) {
		Vector3d newPos = this.coordination.filterVecMutable(
			this.transform(x, y, z)
		);
		if (newPos == null) return;
		double newX = newPos.x;
		double newY = newPos.y;
		double newZ = newPos.z;
		CompoundTag copy = nbt.copy();
		copy.putString("id", entityType);
		this.world.spawnEntity((ServerLevel serverWorld) -> {
			return EntityType.loadEntityRecursive(
				copy, serverWorld, EntitySpawnReason.CHUNK_GENERATION, (Entity entity) -> {
					entity.snapTo(newX, newY, newZ, entity.getYRot(), entity.getXRot());
					return entity;
				}
			);
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

	public static record Coordination(SymmetricOffset transformation, BoundingBox mutableArea, BoundingBox immutableArea) {

		public static MutableBlockPos rotate(MutableBlockPos pos, SymmetricOffset rotation) {
			int x = rotation.getX(pos.getX(), pos.getY(), pos.getZ());
			int y = rotation.getY(pos.getX(), pos.getY(), pos.getZ());
			int z = rotation.getZ(pos.getX(), pos.getY(), pos.getZ());
			return pos.set(x, y, z);
		}

		public MutableBlockPos modifyPosUnbounded(MutableBlockPos pos) {
			return rotate(pos, this.transformation);
		}

		public @Nullable MutableBlockPos filterPosMutable(MutableBlockPos pos) {
			return this.mutableArea.isInside(pos) ? pos : null;
		}

		public @Nullable MutableBlockPos filterPosImmutable(MutableBlockPos pos) {
			return this.immutableArea.isInside(pos) ? pos : null;
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

		public static boolean contains(BoundingBox area, double x, double y, double z) {
			return (
				x >= area.minX() && x <= area.maxX() + 1 &&
				y >= area.minY() && y <= area.maxY() + 1 &&
				z >= area.minZ() && z <= area.maxZ() + 1
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