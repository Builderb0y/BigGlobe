package builderb0y.bigglobe.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SymmetricOffsetTest {

	@BeforeAll
	public static void beforeAll() {
		SymmetricOffset.Testing.enabled = true;
	}

	@Test
	public void testRotation() {
		for (Symmetry base : Symmetry.VALUES) {
			SymmetricOffset rotation = new SymmetricOffset(123, 456, 789, base);
			for (Symmetry second : Symmetry.VALUES) {
				assertEquals(rotation, rotation.rotate(second).rotate(second.inverse()));
				assertEquals(rotation, rotation.rotate(second).rotate(second).rotate(second).rotate(second));
			}
		}
	}
}