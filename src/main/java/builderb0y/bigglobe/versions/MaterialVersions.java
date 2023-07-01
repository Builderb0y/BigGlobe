package builderb0y.bigglobe.versions;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;

public class MaterialVersions {

	public static boolean isReplaceable(BlockState state) {
		return state.getMaterial().isReplaceable();
	}

	public static boolean isReplaceableOrPlant(BlockState state) {
		return isReplaceable(state) || state.getMaterial() == Material.PLANT;
	}
}