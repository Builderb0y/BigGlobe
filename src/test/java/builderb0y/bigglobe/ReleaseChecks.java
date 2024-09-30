package builderb0y.bigglobe;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.commands.DevDebugCommand;
import builderb0y.bigglobe.features.SerializableBlockQueue;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.util.Async;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseChecks {

	@SuppressWarnings({ "ConstantAssertArgument", "JavaReflectionMemberAccess" })
	@Test
	void test() {
		try {
			BigGlobeItems.class.getDeclaredField("TEST_ITEM");
			fail("TEST_ITEM should be commented out before release.");
		}
		catch (NoSuchFieldException expected) {}
		assertFalse(BigGlobeScriptedChunkGenerator.WORLD_SLICES);
		assertFalse(Async.DEBUG_SYNC);
		assertFalse(SerializableBlockQueue.DEBUG_ALWAYS_SERIALIZE);
		assertFalse(DevDebugCommand.ENABLED);
	}
}