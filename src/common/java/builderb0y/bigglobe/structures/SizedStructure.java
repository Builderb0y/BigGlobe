package builderb0y.bigglobe.structures;

import net.minecraft.world.level.levelgen.structure.Structure;

/**
implemented by {@link Structure} via mixin.
*/
public interface SizedStructure {

	public abstract int bigglobe_getMaxRadiusInChunks();

	public default int bigglobe_getMaxRadiusInBlocks() {
		return this.bigglobe_getMaxRadiusInChunks() << 4;
	}
}