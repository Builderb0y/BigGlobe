package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screen.world.WorldCreator.WorldType;

@Mixin(WorldType.class)
public class WorldType_ChangeTranslation {

	@ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "generator.custom"))
	private static String bigglobe_modifyTranslation(String original) {
		return "generator.unknown";
	}
}