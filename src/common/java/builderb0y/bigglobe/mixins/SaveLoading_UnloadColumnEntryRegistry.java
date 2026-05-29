package builderb0y.bigglobe.mixins;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.InitConfig;
import net.minecraft.server.WorldLoader.ResultFactory;
import net.minecraft.server.WorldLoader.WorldDataSupplier;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;

@Mixin(WorldLoader.class)
public class SaveLoading_UnloadColumnEntryRegistry {

	@Inject(method = "load", at = @At("HEAD"))
	private static void bigglobe_resetColumnEntryRegistry(
		InitConfig serverConfig,
		WorldDataSupplier<?> loadContextSupplier,
		ResultFactory<?, ?> saveApplierFactory,
		Executor prepareExecutor,
		Executor applyExecutor,
		CallbackInfoReturnable<CompletableFuture<?>> callback
	) {
		builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry.Loading.reset();
	}

	@ModifyReturnValue(method = "load", at = @At("RETURN"))
	private static CompletableFuture<?> bigglobe_finishLoadingColumnEntryRegistry(
		CompletableFuture<?> original
	) {
		return original.whenComplete((Object result, Throwable exception) -> {
			ColumnEntryRegistry.Loading.endLoad(exception == null);
		});
	}
}