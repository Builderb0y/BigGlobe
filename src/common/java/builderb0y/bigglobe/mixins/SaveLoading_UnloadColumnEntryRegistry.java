package builderb0y.bigglobe.mixins;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.ResultFactory;
import net.minecraft.server.packs.resources.CloseableResourceManager;

import builderb0y.bigglobe.columns.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;

@Mixin(WorldLoader.class)
public class SaveLoading_UnloadColumnEntryRegistry {

	@Inject(method = "load", at = @At("HEAD"))
	private static void bigglobe_resetColumnEntryRegistry(
		CallbackInfoReturnable<CompletableFuture<?>> callback
	) {
		ColumnEntryRegistry.Loading.reset();
	}

	@Inject(method = "lambda$load$4", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/WorldLoader$ResultFactory;create(Lnet/minecraft/server/packs/resources/CloseableResourceManager;Lnet/minecraft/server/ReloadableServerResources;Lnet/minecraft/core/LayeredRegistryAccess;Ljava/lang/Object;)Ljava/lang/Object;"))
	private static void bigglobe_finishLoadingColumnEntryRegistry(
		ResultFactory resultFactory,
		CloseableResourceManager resources,
		LayeredRegistryAccess resourcesLoadContext,
		DataLoadOutput worldDataAndRegistries,
		ReloadableServerResources managers,
		CallbackInfoReturnable<Object> callback
	) {
		ColumnEntryRegistry.Loading.endLoad(RegistryAdapter.StreamableLookup.from(managers.fullRegistries().lookup()));
	}
}