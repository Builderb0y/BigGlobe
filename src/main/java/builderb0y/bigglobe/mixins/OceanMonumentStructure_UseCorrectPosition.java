package builderb0y.bigglobe.mixins;

import java.util.Optional;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.gen.structure.OceanMonumentStructure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

@Mixin(OceanMonumentStructure.class)
public class OceanMonumentStructure_UseCorrectPosition {

	@ModifyReturnValue(method = "getStructurePosition", at = @At("TAIL"))
	private Optional<StructurePosition> bigglobe_useCorrectPosition(Optional<StructurePosition> original) {
		return original.map((StructurePosition position) -> {
			return new StructurePosition(position.position().withY(39), position.generator());
		});
	}
}