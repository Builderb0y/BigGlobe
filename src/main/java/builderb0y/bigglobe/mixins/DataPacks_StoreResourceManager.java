package builderb0y.bigglobe.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.WorldLoader.PackConfig;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.BigGlobeMod.DelegatingResourceManager;

@Mixin(PackConfig.class)
public class DataPacks_StoreResourceManager {

	@Inject(method = "createResourceManager", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/mojang/datafixers/util/Pair;", remap = false))
	private void bigglobe_storeResourceManager(CallbackInfoReturnable<Pair<WorldDataConfiguration, CloseableResourceManager>> callback, @Local CloseableResourceManager manager) {
		if (!(BigGlobeMod.currentResourceManager instanceof DelegatingResourceManager)) {
			BigGlobeMod.currentResourceManager = manager;
		}
	}
}