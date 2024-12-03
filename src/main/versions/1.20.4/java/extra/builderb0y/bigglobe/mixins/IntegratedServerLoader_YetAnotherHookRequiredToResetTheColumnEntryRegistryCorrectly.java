package builderb0y.bigglobe.mixins;

import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage.Session;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoader_YetAnotherHookRequiredToResetTheColumnEntryRegistryCorrectly {

	@Inject(method = "start(Lnet/minecraft/world/level/storage/LevelStorage$Session;Lcom/mojang/serialization/Dynamic;ZZLjava/lang/Runnable;)V", at = @At("HEAD"))
	private void bigglobe_resetColumnRegistry(Session session, Dynamic<?> levelProperties, boolean safeMode, boolean canShowBackupPrompt, Runnable onCancel, CallbackInfo callback) {
		ColumnEntryRegistry.Loading.reset();
	}
}