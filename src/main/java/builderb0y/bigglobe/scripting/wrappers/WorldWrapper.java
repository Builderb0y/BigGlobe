package builderb0y.bigglobe.scripting.wrappers;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;

import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WorldWrapper {

	public static final TypeInfo TYPE = type(WorldWrapper.class);

	public final StructureWorldAccess world;
	public final Coordination coordination;
	public final BlockPos.Mutable pos;
	public final Permuter permuter;
	public final WorldColumn biomeColumn;

	public WorldWrapper(StructureWorldAccess world, Permuter permuter, Coordination coordination) {
		this.world = world;
		this.coordination = coordination;
		this.pos = new BlockPos.Mutable();
		this.permuter = permuter;
		this.biomeColumn = WorldColumn.forWorld(world, 0, 0);
	}

	public @Nullable BlockPos pos(int x, int y, int z) {
		return this.coordination.modifyPos(this.pos.set(x, y, z));
	}

	public long getSeed() {
		return this.world.getSeed();
	}

	public BlockState getBlockState(int x, int y, int z) {
		BlockPos pos = this.pos(x, y, z);
		return pos == null ? BlockStates.AIR : this.coordination.unmodifyState(this.world.getBlockState(pos));
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			state = this.coordination.modifyState(state);
			WorldUtil.setBlockState(this.world, pos, state, Block.NOTIFY_ALL);
			if (!state.getFluidState().isEmpty()) {
				this.world.scheduleFluidTick(
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
		int tmp;
		if (maxX < minX) {
			tmp  = minX;
			minX = maxX;
			maxX = tmp;
		}
		if (maxY < minY) {
			tmp  = minY;
			minY = maxY;
			maxY = tmp;
		}
		if (maxZ < minZ) {
			tmp  = minZ;
			minZ = maxZ;
			maxZ = tmp;
		}
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					this.setBlockState(x, y, z, state);
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
		BlockPos pos = this.pos(x, y, z);
		if (pos != null) {
			this.biomeColumn.setPos(x, z);
			return new BiomeEntry(this.biomeColumn.getBiome(y));
		}
		else {
			return new BiomeEntry(
				this
				.world
				.getRegistryManager()
				.get(RegistryKeys.BIOME)
				.getEntry(BiomeKeys.PLAINS)
				.orElseThrow(() -> new UnregisteredObjectException("Missing default plains biome"))
			);
		}
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
		Identifier identifier = new Identifier(entityType);
		if (Registries.ENTITY_TYPE.containsId(identifier)) {
			Entity entity = Registries.ENTITY_TYPE.get(identifier).create(this.world.toServerWorld());
			if (entity != null) {
				entity.refreshPositionAndAngles(x, y, z, entity.getYaw(), entity.getPitch());
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
		NbtCompound copy = nbt.copy();
		copy.putString("id", entityType);
		Entity entity = EntityType.loadEntityWithPassengers(copy, this.world.toServerWorld(), entity1 -> {
			entity1.refreshPositionAndAngles(x, y, z, entity1.getYaw(), entity1.getPitch());
			return entity1;
		});
		if (entity != null) this.world.spawnEntityAndPassengers(entity);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { " + this.world + " }";
	}

	public static record Coordination(int x, int z, BlockRotation rotation, BlockBox area) {

		public static BlockPos.Mutable rotate(BlockPos.Mutable pos, int centerX, int centerZ, BlockRotation rotation) {
			int x1 = pos.getX() - centerX;
			int z1 = pos.getZ() - centerZ;
			int x2, z2;
			switch (rotation) {
				case NONE -> { x2 = x1; z2 = z1; }
				case CLOCKWISE_90 -> { x2 = -z1; z2 = x1; }
				case CLOCKWISE_180 -> { x2 = -x1; z2 = -z1; }
				case COUNTERCLOCKWISE_90 -> { x2 = z1; z2 = -x1; }
				default -> throw new AssertionError(rotation);
			}
			int x3 = x2 + centerX;
			int z3 = z2 + centerZ;
			return pos.setX(x3).setZ(z3);
		}

		public BlockPos.@Nullable Mutable modifyPos(BlockPos.Mutable pos) {
			return this.area.contains(rotate(pos, this.x, this.z, this.rotation)) ? pos : null;
		}

		public BlockState modifyState(BlockState state) {
			return state.rotate(this.rotation);
		}

		public BlockState unmodifyState(BlockState state) {
			return state.rotate(switch (this.rotation) {
				case NONE -> BlockRotation.NONE;
				case CLOCKWISE_90 -> BlockRotation.COUNTERCLOCKWISE_90;
				case CLOCKWISE_180 -> BlockRotation.CLOCKWISE_180;
				case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
			});
		}
	}
}