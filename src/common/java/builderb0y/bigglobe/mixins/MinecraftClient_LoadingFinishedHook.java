package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.rendering2.hyperspace.HyperspaceRenderer;
import builderb0y.bigglobe.rendering2.waypoints.WaypointRenderer;

@Mixin(Minecraft.class)
public class MinecraftClient_LoadingFinishedHook {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void bigglobe_finishLoading(GameConfig args, CallbackInfo callback) {
		if (BigGlobeMod.MIXIN_AUDIT) {
			BigGlobeMod.LOGGER.info("Performing audit...");
			MixinEnvironment.getCurrentEnvironment().audit();
			BigGlobeMod.LOGGER.info("Audit complete.");
		}
		ColumnEntryRegistry.Loading.reset(); //fix compatibility with veil.
		WaypointRenderer.init();
		HyperspaceRenderer.init();
	}
}