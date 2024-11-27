package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;

@Mixin(MinecraftClient.class)
public class MinecraftClient_LoadingFinishedHook {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void bigglobe_finishLoading(RunArgs args, CallbackInfo callback) {
		if (BigGlobeMod.MIXIN_AUDIT) {
			BigGlobeMod.LOGGER.info("Performing audit...");
			MixinEnvironment.getCurrentEnvironment().audit();
			BigGlobeMod.LOGGER.info("Audit complete.");
		}
		ColumnEntryRegistry.Loading.reset(); //fix compatibility with veil.
	}
}