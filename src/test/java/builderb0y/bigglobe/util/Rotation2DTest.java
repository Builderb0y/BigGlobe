package builderb0y.bigglobe.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.util.BlockRotation;

import static org.junit.jupiter.api.Assertions.*;

public class Rotation2DTest {

	@BeforeAll
	public static void beforeAll() {
		Rotation2D.Testing.enabled = true;
	}

	@Test
	public void testRotation() {
		for (BlockRotation base : Directions.ROTATIONS) {
			Rotation2D rotation = new Rotation2D(123, 456, base);
			for (BlockRotation second : Directions.ROTATIONS) {
				assertEquals(rotation, rotation.rotate(second).rotate(switch (second) {
					case NONE -> BlockRotation.NONE;
					case CLOCKWISE_90 -> BlockRotation.COUNTERCLOCKWISE_90;
					case CLOCKWISE_180 -> BlockRotation.CLOCKWISE_180;
					case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
				}));
				assertEquals(rotation, rotation.rotate(second).rotate(second).rotate(second).rotate(second));
			}
		}
	}
}