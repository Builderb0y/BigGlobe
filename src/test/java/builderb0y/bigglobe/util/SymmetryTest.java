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

	@Test
	public void testCompositionOrder() {
		for (Symmetry first : Symmetry.VALUES) {
			if (first.isFlipped()) for (Symmetry second : Symmetry.VALUES) {
				if (second.isFlipped()) {
					Symmetry third = first.andThen(second);
					assertSame(second, first.andThen(third));
				}
			}
		}
	}

	@Test
	public void testManual() {
		record Point(int x, int z) {

			public Point transform(Symmetry symmetry) {
				Point that = new Point(symmetry.getX(this.x, this.z), symmetry.getZ(this.x, this.z));
				assertEquals((double)(that.x), symmetry.getX((double)(this.x), (double)(this.z)));
				assertEquals((double)(that.z), symmetry.getZ((double)(this.x), (double)(this.z)));
				return that;
			}
		}
		Point
			rightUp   = new Point( 2, -1),
			upRight   = new Point( 1, -2),
			upLeft    = new Point(-1, -2),
			leftUp    = new Point(-2, -1),
			leftDown  = new Point(-2,  1),
			downLeft  = new Point(-1,  2),
			downRight = new Point( 1,  2),
			rightDown = new Point( 2,  1);
		assertEquals(rightUp,   rightUp.transform(Symmetry.IDENTITY));
		assertEquals(downRight, rightUp.transform(Symmetry.ROTATE_90));
		assertEquals(leftDown,  rightUp.transform(Symmetry.ROTATE_180));
		assertEquals(upLeft,    rightUp.transform(Symmetry.ROTATE_270));
		assertEquals(leftUp,    rightUp.transform(Symmetry.FLIP_0));
		assertEquals(downLeft,  rightUp.transform(Symmetry.FLIP_45));
		assertEquals(rightDown, rightUp.transform(Symmetry.FLIP_90));
		assertEquals(upRight,   rightUp.transform(Symmetry.FLIP_135));
	}
}