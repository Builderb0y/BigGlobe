package builderb0y.bigglobe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SymmetryTest {

	@Test
	public void testInverse() {
		for (Symmetry first : Symmetry.VALUES) {
			assertSame(Symmetry.IDENTITY, first.andThen(first.inverse()));
			for (Symmetry second : Symmetry.VALUES) {
				assertSame(first, first.andThen(second).andThen(second.inverse()));
			}
		}
	}

	@Test
	public void test4x() {
		for (Symmetry first : Symmetry.VALUES) {
			assertSame(Symmetry.IDENTITY, first.andThen(first).andThen(first).andThen(first));
			for (Symmetry second : Symmetry.VALUES) {
				assertSame(first, first.andThen(second).andThen(second).andThen(second).andThen(second));
			}
		}
	}
}