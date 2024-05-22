package builderb0y.bigglobe.columns.scripted.entries;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnEntryTest {

	@Test
	public void testRelativize() {
		ColumnEntry.Testing.TESTING = true;

		assertEquals("global", chop("global", "something"));
		assertEquals("global", chop("dim/global", "dim/something"));
		assertEquals("global", chop("dim/thing/global", "dim/thing/something"));

		assertEquals("dim/global", chop("dim/global", "something"));
		assertEquals("thing/global", chop("dim/thing/global", "dim/something"));

		//different folders.
		assertNull(chop("dim1/global", "dim2/global"));
		assertNull(chop("dim1/thing/global", "dim2/thing/global"));
		assertNull(chop("dim/thing1/global", "dim/thing2/global"));

		//no back-tracking.
		assertNull(chop("global", "dim/global"));
		assertNull(chop("dim/global", "dim/thing/global"));
	}

	public static @Nullable String chop(String selfPath, String callerPath) {
		int start = ColumnEntry.relativize(selfPath, callerPath);
		return start >= 0 ? selfPath.substring(start) : null;
	}
}