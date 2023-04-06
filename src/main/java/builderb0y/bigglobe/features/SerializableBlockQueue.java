package builderb0y.bigglobe.features;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RegistryWorldView;
import net.minecraft.world.WorldAccess;

import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.trees.TreeSpecialCases;
import builderb0y.bigglobe.util.WorldUtil;

public class SerializableBlockQueue extends BlockQueue {

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

	@Override
	public void queueBlock(long pos, BlockState state) {
		super.queueBlock(pos, state);
		int x = BlockPos.unpackLongX(pos);
		int y = BlockPos.unpackLongY(pos);
		int z = BlockPos.unpackLongZ(pos);
		//cursed, but pretty, formatting.
		if (x < this.minX) this.minX = x; else
		if (x > this.maxX) this.maxX = x;
		if (y < this.minY) this.minY = y; else
		if (y > this.maxY) this.maxY = y;
		if (z < this.minZ) this.minZ = z; else
		if (z > this.maxZ) this.maxZ = z;
	}

	@Override
	public void placeQueuedBlocks(WorldAccess world) {
		BlockPos pos = new BlockPos(this.centerX, this.centerY, this.centerZ);
		WorldUtil.setBlockState(world, pos, BlockStates.DELAYED_GENERATION, this.flags);
		DelayedGenerationBlockEntity blockEntity = WorldUtil.getBlockEntity(world, pos, DelayedGenerationBlockEntity.class);
		if (blockEntity != null) blockEntity.blockQueue = this;
	}

	public void actuallyPlaceQueuedBlocks(WorldAccess world) {
		super.placeQueuedBlocks(world);
	}

	public boolean hasSpace(WorldAccess world) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (LongIterator iterator = this.queuedBlocks.keySet().iterator(); iterator.hasNext();) {
			BlockState state = world.getBlockState(pos.set(iterator.nextLong()));
			if (state.getMaterial().isReplaceable() || state.getMaterial() == Material.PLANT || TreeSpecialCases.getGroundReplacements().containsKey(state)) {
				continue;
			}
			else {
				return false;
			}
		}
		return true;
	}

	public static SerializableBlockQueue read(RegistryWorldView world, NbtCompound nbt) {
		int flags = nbt.getInt("flags");
		int[] center = nbt.getIntArray("center");
		int centerX = center[0];
		int centerY = center[1];
		int centerZ = center[2];
		SerializableBlockQueue queue = new SerializableBlockQueue(centerX, centerY, centerZ, flags);
		NbtList paletteNBT = nbt.getList("palette", NbtElement.COMPOUND_TYPE);
		ObjectList<BlockState> palette = new ObjectArrayList<>(paletteNBT.size());
		RegistryWrapper<Block> registry = world == null ? Registries.BLOCK.getReadOnlyWrapper() : world.createCommandRegistryWrapper(RegistryKeys.BLOCK);
		for (int index = 0, size = paletteNBT.size(); index < size; index++) {
			palette.add(NbtHelper.toBlockState(registry, paletteNBT.getCompound(index)));
		}
		byte[] blocksNBT = nbt.getByteArray("blocks");
		if ((blocksNBT.length & 3) != 0) {
			throw new IllegalArgumentException("blocks NBT wrong length: " + blocksNBT.length);
		}
		for (int index = 0, length = blocksNBT.length; index < length;) {
			int x = centerX + blocksNBT[index++];
			int y = centerY + blocksNBT[index++];
			int z = centerZ + blocksNBT[index++];
			BlockState state = palette.get(blocksNBT[index++]);
			queue.queueBlock(BlockPos.asLong(x, y, z), state);
		}
		return queue;
	}

	public NbtCompound toNBT() {
		NbtCompound nbt = new NbtCompound();
		nbt.putIntArray("center", new int[] { this.centerX, this.centerY, this.centerZ });
		nbt.putInt("flags", this.flags);
		Object2ByteMap<BlockState> palette = new Object2ByteOpenHashMap<>(16);
		NbtList paletteNBT = new NbtList();
		for (BlockState state : this.queuedBlocks.values()) {
			if (!palette.containsKey(state)) {
				palette.put(state, BigGlobeMath.toUnsignedByteExact(palette.size()));
				paletteNBT.add(NbtHelper.fromBlockState(state));
			}
		}
		nbt.put("palette", paletteNBT);
		ByteArrayList blocksNBT = new ByteArrayList(this.queuedBlocks.size() << 2);
		for (ObjectBidirectionalIterator<Long2ObjectMap.Entry<BlockState>> iterator = this.queuedBlocks.long2ObjectEntrySet().fastIterator(); iterator.hasNext();) {
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
		nbt.put("blocks", new NbtByteArray(blocksNBT.elements()));
		return nbt;
	}
}