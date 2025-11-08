package builderb0y.bigglobe.mixins;

import java.util.concurrent.CompletableFuture;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.world.GeneratorOptionsHolder;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(CreateWorldScreen.class)
public class Dev_CreateWorldScreen_DontCrashOnFailure {

	#if MC_VERSION >= MC_1_21_9
		@WrapOperation(
			method = "show(Lnet/minecraft/client/MinecraftClient;Ljava/lang/Runnable;Ljava/util/function/Function;Lnet/minecraft/client/world/GeneratorOptionsFactory;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/gui/screen/world/CreateWorldCallback;)V",
			at = @At(
				value = "INVOKE",
				target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"
			)
		)
	#elif MC_VERSION >= MC_1_21_2
		@WrapOperation(
			method = "show(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;Ljava/util/function/Function;Lnet/minecraft/client/world/GeneratorOptionsFactory;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/gui/screen/world/CreateWorldCallback;)V",
			at = @At(
				value = "INVOKE",
				target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"
			)
		)
	#else
		@WrapOperation(method = "create(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
	#endif
	private static Object bigglobe_wrapLoading(
		CompletableFuture<GeneratorOptionsHolder> future,
		Operation<GeneratorOptionsHolder> original,
		@Cancellable CallbackInfo callback,
		@Local(index = 0, argsOnly = true) MinecraftClient client,
		#if MC_VERSION >= MC_1_21_9
			@Local(index = 1, argsOnly = true) Runnable closer
		#else
			@Local(index = 1, argsOnly = true) Screen parentScreen
		#endif
	) {
		if (callback.isCancelled()) return null; //https://github.com/LlamaLad7/MixinExtras/wiki/Cancellable#niche-interactions-with-wrapoperation
		try {
			return original.call(future);
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Caught exception from loading built-in data packs:", exception);
			#if MC_VERSION >= MC_1_21_9
				closer.run();
			#else
				client.setScreen(parentScreen);
			#endif
			callback.cancel();
			return null;
		}
	}
}