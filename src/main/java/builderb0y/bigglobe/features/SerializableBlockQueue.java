package builderb0y.bigglobe.features;

import java.util.Map;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.MaterialVersions;

public class SerializableBlockQueue extends BlockQueue {

	/**
	used for testing serialization logic;
	enabling this flag will serialize and deserialize every
	queue before storing it in a delayed generation block.
	that way, if there's any issues with it, they will
	become immediately obvious, and you don't need to wait
	for the delayed generation block to unload and reload.
	*/
	public static final boolean DEBUG_ALWAYS_SERIALIZE = false;

	public @NotNull Long2ObjectLinkedOpenHashMap<BlockState> queuedReplacements = new Long2ObjectLinkedOpenHashMap<>(64);

	public int centerX, centerY, centerZ;
	public int minX, minY, minZ, maxX, maxY, maxZ;

	public SerializableBlockQueue(int centerX, int centerY, int centerZ, int flags) {
		super(flags);
		this.centerX = centerX;
		this.centerY = centerY;
		this.centerZ = centerZ;
		this.minX    = centerX;
		this.minY    = centerY;
		this.minZ    = centerZ;
		this.maxX    = centerX;
		this.maxY    = centerY;
		this.maxZ    = centerZ;
	}

	public SerializableBlockQueue(int centerX, int centerY, int centerZ, boolean causeBlockUpdates) {
		this(centerX, centerY, centerZ, causeBlockUpdates ? Block.NOTIFY_ALL : Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
	}

	@Override
	public void queueBlock(long pos, BlockState state) {
		super.queueBlock(pos, state);
		int x = BlockPos.unpackLongX(pos);
		int y = BlockPos.unpackLongY(pos);
		int z = BlockPos.unpackLongZ(pos);
		this.minX = Math.min(this.minX, x);
		this.minY = Math.min(this.minY, y);
		this.minZ = Math.min(this.minZ, z);
		this.maxX = Math.max(this.maxX, x);
		this.maxY = Math.max(this.maxY, y);
		this.maxZ = Math.max(this.maxZ, z);
	}

	@Override
	public void queueReplacement(long pos, BlockState from, BlockState to) {
		super.queueReplacement(pos, from, to);
		this.queuedReplacements.put(pos, from);
	}

	@Override
	public void placeQueuedBlocks(WorldAccess world) {
		BlockPos pos = new BlockPos(this.centerX, this.centerY, this.centerZ);
		BlockState oldState = world.getBlockState(pos);
		BlockEntity oldBlockEntity = world.getBlockEntity(pos);
		NbtCompound oldBlockData = oldBlockEntity == null ? null : oldBlockEntity.createNbt();
		WorldUtil.setBlockState(world, pos, BlockStates.DELAYED_GENERATION, this.flags);
		DelayedGenerationBlockEntity blockEntity = WorldUtil.getBlockEntity(world, pos, DelayedGenerationBlockEntity.class);
		if (blockEntity != null) {
			blockEntity.blockQueue = DEBUG_ALWAYS_SERIALIZE ? read(this.toNBT()) : this;
			blockEntity.oldState = oldState;
			blockEntity.oldBlockData = oldBlockData;
		}
	}

	public void actuallyPlaceQueuedBlocks(WorldAccess world) {
		super.placeQueuedBlocks(world);
	}

	public boolean hasSpace(WorldAccess world) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (LongIterator iterator = this.queuedBlocks.keySet().iterator(); iterator.hasNext();) {
			long longPos = iterator.nextLong();
			BlockState state = world.getBlockState(pos.set(longPos));
			if (!this.canReplace(longPos, state)) {
				return false;
			}
		}
		return true;
	}

	public boolean canReplace(long pos, BlockState state) {
		return (
			canImplicitlyReplace(state) ||
			this.queuedReplacements.get(pos) == state
		);
	}

	public static boolean canImplicitlyReplace(BlockState state) {
		return MaterialVersions.isReplaceableOrPlant(state);
	}

	public static SerializableBlockQueue read(NbtCompound nbt) {
		int flags = nbt.getInt("flags");
		int[] center = nbt.getIntArray("center");
		int centerX = center[0];
		int centerY = center[1];
		int centerZ = center[2];
		SerializableBlockQueue queue = new SerializableBlockQueue(centerX, centerY, centerZ, flags);
		NbtList paletteNBT = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
		ObjectList<BlockState> palette = new ObjectArrayList<>(paletteNBT.size());
		RegistryWrapper<Block> registry = Registries.BLOCK.getReadOnlyWrapper();
		for (int index = 0, size = paletteNBT.size(); index < size; index++) {
			palette.add(NbtHelper.toBlockState(registry, paletteNBT.getCompound(index)));
		}
		readBlocks(centerX, centerY, centerZ, palette, nbt, "blocks", queue::queueBlock);
		readBlocks(centerX, centerY, centerZ, palette, nbt, "replacements", queue.queuedReplacements::put);
		return queue;
	}

	public static void readBlocks(int centerX, int centerY, int centerZ, ObjectList<BlockState> palette, NbtCompound nbt, String key, LongPosStateConsumer adder) {
		byte[] blocksNBT = nbt.getByteArray(key);
		if ((blocksNBT.length & 3) != 0) {
			throw new IllegalArgumentException(key + " NBT wrong length: " + blocksNBT.length);
		}
		for (int index = 0, length = blocksNBT.length; index < length;) {
			int x = centerX + blocksNBT[index++];
			int y = centerY + blocksNBT[index++];
			int z = centerZ + blocksNBT[index++];
			BlockState state = palette.get(blocksNBT[index++]);
			adder.accept(BlockPos.asLong(x, y, z), state);
		}
	}

	@FunctionalInterface
	public static interface LongPosStateConsumer {

		public abstract void accept(long pos, BlockState state);
	}

	public NbtCompound toNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putIntArray("center", new int[] { this.centerX, this.centerY, this.centerZ });
		nbt.putInt("flags", this.flags);
		Object2ByteMap<BlockState> palette = new Object2ByteOpenHashMap<>(16);
		NbtList paletteNBT = new NbtList();
		this.addToPalette(palette, paletteNBT, this.queuedBlocks);
		this.addToPalette(palette, paletteNBT, this.queuedReplacements);
		nbt.put("palette", paletteNBT);
		nbt.put("blocks", this.writeBlocks(palette, this.queuedBlocks));
		nbt.put("replacements", this.writeBlocks(palette, this.queuedReplacements));
		return nbt;
	}

	public void addToPalette(Object2ByteMap<BlockState> palette, NbtList paletteNBT, Long2ObjectMap<BlockState> blocks) {
		for (BlockState state : blocks.values()) {
			if (!palette.containsKey(state)) {
				palette.put(state, BigGlobeMath.toUnsignedByteExact(palette.size()));
				paletteNBT.add(NbtHelper.fromBlockState(state));
			}
		}
	}

	public NbtByteArray writeBlocks(Object2ByteMap<BlockState> palette, Long2ObjectMap<BlockState> blocks) {
		ByteArrayList blocksNBT = new ByteArrayList(blocks.size() << 2);
		for (ObjectIterator<Long2ObjectMap.Entry<BlockState>> iterator = Long2ObjectMaps.fastIterator(blocks); iterator.hasNext();) {
			Long2ObjectMap.Entry<BlockState> entry = iterator.next();
			byte x = BigGlobeMath.toByteExact(BlockPos.unpackLongX(entry.getLongKey()) - this.centerX);
			byte y = BigGlobeMath.toByteExact(BlockPos.unpackLongY(entry.getLongKey()) - this.centerY);
			byte z = BigGlobeMath.toByteExact(BlockPos.unpackLongZ(entry.getLongKey()) - this.centerZ);
			byte id = palette.getByte(entry.getValue());
			blocksNBT.add(x);
			blocksNBT.add(y);
			blocksNBT.add(z);
			blocksNBT.add(id);
		}
		assert blocksNBT.size() == blocksNBT.elements().length;
		return new NbtByteArray(blocksNBT.elements());
	}

	@Override
	public Object[] intellij_childrenArray() {
		return new Object[] {
			Map.entry("flags", this.flags),
			Map.entry("queuedBlocks", intellij_decodePositions(this.queuedBlocks)),
			Map.entry("queuedReplacements", intellij_decodePositions(this.queuedReplacements))
		};
	}
}