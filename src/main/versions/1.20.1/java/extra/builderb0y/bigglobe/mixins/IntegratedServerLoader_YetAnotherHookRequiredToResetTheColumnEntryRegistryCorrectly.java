package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServerLoader;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoader_YetAnotherHookRequiredToResetTheColumnEntryRegistryCorrectly {

	@Inject(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;ZZ)V", at = @At("HEAD"))
	private void bigglobe_resetColumnRegistry(Screen parent, String levelName, boolean safeMode, boolean canShowBackupPrompt, CallbackInfo callback) {
		ColumnEntryRegistry.Loading.reset();
	}
}