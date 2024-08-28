package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.server.SaveLoading.DataPacks;

import builderb0y.bigglobe.BigGlobeMod;

@Mixin(DataPacks.class)
public class DataPacks_StoreResourceManager {

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/mojang/datafixers/util/Pair;"))
	private void bigglobe_storeResourceManager(CallbackInfoReturnable<Pair<DataConfiguration, LifecycledResourceManager>> callback, @Local LifecycledResourceManager manager) {
		if (BigGlobeMod.currentResourceFactory == null) {
			BigGlobeMod.currentResourceFactory = manager;
		}
	}
}