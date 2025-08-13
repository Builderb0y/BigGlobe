package builderb0y.bigglobe.features;

import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.*;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blockEntities.DelayedGenerationBlockEntity;
import builderb0y.bigglobe.blocks.BigGlobeBlockTags;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.versions.*;

import net.minecraft.registry.RegistryWrapper;

@UseCoder(name = "CODER", in = SerializableBlockQueue.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
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

	public static final AutoCoder<SerializableBlockQueue> CODER = new NamedCoder<>(ReifiedType.from(SerializableBlockQueue.class)) {

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable SerializableBlockQueue decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.data.isEmpty()) return null;
			MapData map = context.data.tryAsMap();
			if (map == null) throw context.notA("map");
			return read(map);
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, SerializableBlockQueue> context) throws EncodeException {
			SerializableBlockQueue queue = context.object;
			return queue == null ? EmptyData.INSTANCE : queue.toData();
		}
	};

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
		NbtCompound oldBlockData = oldBlockEntity == null ? null : BlockEntityVersions.writeToNbt(oldBlockEntity);
		WorldUtil.setBlockState(world, pos, BlockStates.DELAYED_GENERATION, this.flags);
		DelayedGenerationBlockEntity blockEntity = WorldUtil.getBlockEntity(world, pos, DelayedGenerationBlockEntity.class);
		if (blockEntity != null) {
			blockEntity.blockQueue = DEBUG_ALWAYS_SERIALIZE ? read(this.toData()) : this;
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
		return BlockStateVersions.isReplaceable(state) || state.isIn(BigGlobeBlockTags.PLANTS);
	}

	public static SerializableBlockQueue read(MapData data) {
		int flags = data.get("flags").getAsIntOr(Block.NOTIFY_ALL);
		IntListData center = data.get("center").tryAsIntList();
		if (center == null || center.size() != 3) throw new IllegalArgumentException("Malformed center");
		int centerX = center.getInt(0);
		int centerY = center.getInt(1);
		int centerZ = center.getInt(2);
		SerializableBlockQueue queue = new SerializableBlockQueue(centerX, centerY, centerZ, flags);
		ListData paletteData = data.get("palette").tryAsList();
		if (paletteData == null) throw new IllegalArgumentException("Malformed palette");
		ObjectList<BlockState> palette = new ObjectArrayList<>(paletteData.size());
		BetterRegistry<Block> blockRegistry = BigGlobeMod.getRegistry(RegistryKeys.BLOCK);
		for (Data element : paletteData) {
			StringData string = element.tryAsString();
			if (string != null) {
				palette.add(BlockStateCoder.decodeState(blockRegistry, string.value).unwrapEager(BigGlobeMod.LOGGER::warn, IllegalArgumentException::new).state());
			}
			else {
				throw new IllegalArgumentException("Block state is not encoded as a string: " + element);
			}
		}
		readBlocks(centerX, centerY, centerZ, palette, data, "blocks", queue::queueBlock);
		readBlocks(centerX, centerY, centerZ, palette, data, "replacements", queue.queuedReplacements::put);
		ListData blockEntities = data.get("blockEntities").tryAsList();
		if (blockEntities != null && !blockEntities.isEmpty()) {
			for (Data blockEntityElement : blockEntities) {
				MapData blockEntityData = blockEntityElement.tryAsMap();
				if (blockEntityData != null) {
					AbstractNumberData x, y, z;
					if ((x = blockEntityData.get("x").tryAsNumber()) == null) throw new IllegalArgumentException("Malformed x");
					if ((y = blockEntityData.get("y").tryAsNumber()) == null) throw new IllegalArgumentException("Malformed y");
					if ((z = blockEntityData.get("z").tryAsNumber()) == null) throw new IllegalArgumentException("Malformed z");
					BlockPos pos = new BlockPos(x.intValue(), y.intValue(), z.intValue());
					BlockState state = queue.queuedBlocks.get(pos.asLong());
					if (state != null && state.hasBlockEntity()) {
						BlockEntity blockEntity = BlockEntityVersions.createFromNbt(pos, state, (NbtCompound)(blockEntityData.convert(NbtOps.INSTANCE)));
						if (blockEntity != null) queue.queueBlockEntity(pos, blockEntity);
					}
				}
				else {
					throw new IllegalArgumentException("Non-compound block entity");
				}
			}
		}
		return queue;
	}

	public static void readBlocks(int centerX, int centerY, int centerZ, ObjectList<BlockState> palette, MapData data, String key, LongPosStateConsumer adder) {
		ByteListData byteArray = data.get(key).tryAsByteList();
		if (byteArray == null || (byteArray.size() & 3) != 0) throw new IllegalArgumentException("Malformed " + key);
		for (int index = 0, length = byteArray.size(); index < length;) {
			int x = centerX + byteArray.getByte(index++);
			int y = centerY + byteArray.getByte(index++);
			int z = centerZ + byteArray.getByte(index++);
			BlockState state = palette.get(Byte.toUnsignedInt(byteArray.getByte(index++)));
			adder.accept(BlockPos.asLong(x, y, z), state);
		}
	}

	@FunctionalInterface
	public static interface LongPosStateConsumer {

		public abstract void accept(long pos, BlockState state);
	}

	public MapData toData() {
		MapData data = new MapData();
		data.putIntList("center", this.centerX, this.centerY, this.centerZ);
		data.putInt("flags", this.flags);
		Object2ByteMap<BlockState> palette = new Object2ByteOpenHashMap<>(16);
		ListData paletteData = new ListData();
		this.addToPalette(palette, paletteData, this.queuedBlocks);
		this.addToPalette(palette, paletteData, this.queuedReplacements);
		data.put("palette", paletteData);
		data.put("blocks", this.writeBlocks(palette, this.queuedBlocks));
		data.put("replacements", this.writeBlocks(palette, this.queuedReplacements));
		if (!this.queuedBlockEntities.isEmpty()) {
			ListData blockEntities = new ListData();
			for (BlockEntity blockEntity : this.queuedBlockEntities.values()) {
				blockEntities.value.add(new UnknownData<>(NbtOps.INSTANCE, BlockEntityVersions.writeToNbt(blockEntity)));
			}
			data.put("blockEntities", blockEntities);
		}
		return data;
	}

	public void addToPalette(Object2ByteMap<BlockState> palette, ListData paletteData, Long2ObjectMap<BlockState> blocks) {
		for (BlockState state : blocks.values()) {
			if (!palette.containsKey(state)) {
				palette.put(state, BigGlobeMath.toUnsignedByteExact(palette.size()));
				paletteData.value.add(new StringData(BlockArgumentParser.stringifyBlockState(state)));
			}
		}
	}

	public ByteListData writeBlocks(Object2ByteMap<BlockState> palette, Long2ObjectMap<BlockState> blocks) {
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
		return new ByteListData(blocksNBT);
	}

	@Override
	public Object[] intellij_childrenArray() {
		return new Object[] {
			Map.entry("flags", this.flags),
			Map.entry("queuedBlocks", intellij_decodePositions(this.queuedBlocks)),
			Map.entry("queuedReplacements", intellij_decodePositions(this.queuedReplacements)),
			Map.entry("queuedBlockEntities", intellij_decodePositions(this.queuedBlockEntities))
		};
	}
}