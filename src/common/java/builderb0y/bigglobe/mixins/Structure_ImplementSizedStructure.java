package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.*;

import builderb0y.bigglobe.structures.SizedStructure;

@Mixin(Structure.class)
public class Structure_ImplementSizedStructure implements SizedStructure {

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		Class<?> clazz = this.getClass();
		if (clazz == BuriedTreasureStructure.class) return 0;
		if (clazz == DesertPyramidStructure.class) return 2;
		//end cities sometimes generate cut-off, so increase their search radius.
		if (clazz == EndCityStructure.class) return 12;
		if (clazz == IglooStructure.class) return 1;
		if (clazz == JungleTempleStructure.class) return 1;
		if (clazz == NetherFossilStructure.class) return 1;
		if (clazz == OceanMonumentStructure.class) return 4;
		if (clazz == OceanRuinStructure.class) return 4;
		if (clazz == RuinedPortalStructure.class) return 2;
		if (clazz == ShipwreckStructure.class) return 2;
		if (clazz == SwampHutStructure.class) return 1;
		return 8;
	}
}