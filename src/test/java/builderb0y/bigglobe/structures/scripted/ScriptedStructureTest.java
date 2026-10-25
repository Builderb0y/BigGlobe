package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

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
			BoundingBox oldBox = new BoundingBox(x - size, y - size, z - size, x + size, y + size, z + size);
			BlockPos.MutableBlockPos pos1 = Coordination.rotate(
				new BlockPos.MutableBlockPos(
					oldBox.minX(),
					oldBox.minY(),
					oldBox.minZ()
				),
				offset
			);
			BlockPos.MutableBlockPos pos2 = Coordination.rotate(
				new BlockPos.MutableBlockPos(
					oldBox.maxX(),
					oldBox.maxY(),
					oldBox.maxZ()
				),
				offset
			);
			BoundingBox newBox = WorldUtil.createBlockBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
			assertEquals(oldBox, newBox);
		}
	}
}