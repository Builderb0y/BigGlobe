package builderb0y.bigglobe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.itemdefs.BigGlobeItems;
import builderb0y.bigglobe.util.Async;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseChecks {

	@Test
	@SuppressWarnings({ "ConstantAssertArgument", "JavaReflectionMemberAccess" })
	public void test() {
		try {
			BigGlobeItems.class.getDeclaredField("TEST_ITEM");
			fail("TEST_ITEM should be commented out before release.");
		}
		catch (NoSuchFieldException expected) {}
		assertFalse(BigGlobeScriptedChunkGenerator.WORLD_SLICES);
		assertFalse(Async.DEBUG_SYNC);
	}

	@Test
	public void ensureJSpecifyNotUsed() throws IOException {
		for (File sourceSet : new File("src").listFiles()) {
			scanRecursive(new File(sourceSet, "java"));
		}
	}

	public static void scanRecursive(File root) throws IOException {
		File[] children = root.listFiles();
		if (children != null) {
			for (File child : children) {
				scanRecursive(child);
			}
		}
		else if (root.getPath().endsWith(".java") && !root.getName().equals("ReleaseChecks.java")) {
			if (Files.readString(root.toPath()).contains("jspecify")) {
				fail(root.getPath());
			}
		}
	}
}