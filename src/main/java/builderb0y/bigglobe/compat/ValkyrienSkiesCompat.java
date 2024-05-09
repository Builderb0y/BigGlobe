package builderb0y.bigglobe.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import net.minecraft.util.math.ChunkPos;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;

public class ValkyrienSkiesCompat {

	public static final MethodHandle SHIPYARD;
	static {
		MethodHandle handle;
		try {
			Class<?> allocator = Class.forName("org.valkyrienskies.mod.common.VS2ChunkAllocator");
			handle = MethodHandles.lookup().findVirtual(allocator, "isChunkInShipyardCompanion", MethodType.methodType(boolean.class, int.class, int.class));
			Object instance = allocator.getDeclaredField("INSTANCE").get(null);
			handle = MethodHandles.insertArguments(handle, 0, instance);
			BigGlobeMod.LOGGER.info("Valkyrian skies compatibility enabled.");
		}
		catch (Exception exception) {
			handle = MethodHandles.constant(boolean.class, false);
			handle = MethodHandles.dropArguments(handle, 0, int.class, int.class);
			BigGlobeMod.LOGGER.info("Valkyrian skies compatibility disabled: " + exception);
		}
		SHIPYARD = handle;
	}

	public static boolean isInShipyard(int chunkX, int chunkY) {
		try {
			return (boolean)(SHIPYARD.invokeExact(chunkX, chunkY));
		}
		catch (Throwable exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static boolean isInShipyard(ChunkPos pos) {
		return isInShipyard(pos.x, pos.z);
	}
}