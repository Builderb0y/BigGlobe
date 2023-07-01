package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.gen.structure.EndCityStructure;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;

@Mixin(EndCityStructure.class)
public class EndCityStructure_UnHardcodeMinimumY {

	@ModifyConstant(method = "getStructurePosition", constant = @Constant(intValue = 60))
	private int bigglobe_replaceMinimumY(int oldValue, Structure.Context context) {
		if (context.chunkGenerator() instanceof BigGlobeChunkGenerator) {
			return context.chunkGenerator().getMinimumY() + 1;
		}
		else {
			return oldValue;
		}
	}
}