package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.WoodlandMansionStructure;

@Mixin(WoodlandMansionStructure.class)
public class WoodlandMansionStructure_DontHardCodeSeaLevel {

	@ModifyConstant(method = "getStructurePosition", constant = @Constant(intValue = 60))
	private int bigglobe_getActualSeaLevel(int sixty, Structure.Context context) {
		return context.chunkGenerator().getSeaLevel() - 3;
	}
}