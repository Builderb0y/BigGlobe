package builderb0y.bigglobe.mixins;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WoodlandMansionStructure.class)
public class WoodlandMansionStructure_DontHardCodeSeaLevel {

	@ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = 60))
	private int bigglobe_getActualSeaLevel(int sixty, Structure.GenerationContext context) {
		return context.chunkGenerator().getSeaLevel() - 3;
	}
}