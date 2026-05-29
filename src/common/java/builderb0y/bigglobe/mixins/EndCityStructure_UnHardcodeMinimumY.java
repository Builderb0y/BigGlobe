package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;

import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

@Mixin(EndCityStructure.class)
public class EndCityStructure_UnHardcodeMinimumY {

	@ModifyConstant(method = "findGenerationPoint", constant = @Constant(intValue = 60))
	private int bigglobe_replaceMinimumY(int oldValue, Structure.GenerationContext context) {
		if (context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator) {
			return generator.getMinY() + 1;
		}
		else {
			return oldValue;
		}
	}
}