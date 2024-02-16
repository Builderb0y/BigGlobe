package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptedStructureTest {

	@Test
	public void testSymmetrify() {
		SymmetricOffset.Testing.enabled = true;
		RandomGenerator random = new Permuter(12345L);
		for (int index = 0; index < 1000; index++) {
			int x = random.nextInt(-100, 101);
			int y = random.nextInt(-100, 101);
			int z = random.nextInt(-100, 101);
			int size = random.nextInt(10);
			SymmetricOffset offset = SymmetricOffset.IDENTITY.rotateAround(x, z, Symmetry.VALUES[random.nextInt(8)]);
			BlockBox oldBox = new BlockBox(x - size, y - size, z - size, x + size, y + size, z + size);
			BlockPos.Mutable pos1 = Coordination.rotate(
				new BlockPos.Mutable(
					oldBox.getMinX(),
					oldBox.getMinY(),
					oldBox.getMinZ()
				),
				offset
			);
			BlockPos.Mutable pos2 = Coordination.rotate(
				new BlockPos.Mutable(
					oldBox.getMaxX(),
					oldBox.getMaxY(),
					oldBox.getMaxZ()
				),
				offset
			);
			BlockBox newBox = WorldUtil.createBlockBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
			assertEquals(oldBox, newBox);
		}
	}
}