package builderb0y.bigglobe.mixins;

import java.util.concurrent.CompletableFuture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(CreateWorldScreen.class)
public class Dev_CreateWorldScreen_DontCrashOnFailure {

	@WrapOperation(
		method = "openCreateWorldScreen(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Ljava/util/function/Function;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContextMapper;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"
		)
	)

	private static Object bigglobe_wrapLoading(
		CompletableFuture<WorldCreationContext> future,
		Operation<WorldCreationContext> original,
		@Cancellable CallbackInfo callback,
		@Local(index = 0, argsOnly = true) Minecraft client,

		@Local(index = 1, argsOnly = true) Runnable closer

	) {
		if (callback.isCancelled()) return null; //https://github.com/LlamaLad7/MixinExtras/wiki/Cancellable#niche-interactions-with-wrapoperation
		try {
			return original.call(future);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Caught exception from loading built-in data packs:", exception);

			closer.run();

			callback.cancel();
			return null;
		}
	}
}