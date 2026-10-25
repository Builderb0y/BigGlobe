package builderb0y.bigglobe.mixins;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WorldTypeEntry.class)
public class WorldType_ChangeTranslation {

	@ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "generator.custom"))
	private static String bigglobe_modifyTranslation(String original) {
		return "generator.unknown";
	}
}