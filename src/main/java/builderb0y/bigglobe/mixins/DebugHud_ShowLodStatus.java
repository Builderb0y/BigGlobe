package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.rendering.lods.LodSystem;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;

@Mixin(DebugHud.class)
public class DebugHud_ShowLodStatus {

	#if MC_VERSION < MC_1_21_9

		@Inject(method = "getRightText", at = @At("RETURN"))
		private void bigglobe_showLodStatus(CallbackInfoReturnable<List<String>> callback) {
			LodSystem system = LodSystemHolder.of(MinecraftClient.getInstance().worldRenderer).bigglobe_getLodSystem();
			if (system != null) {
				int meshedNodes = LodSystem.countMeshes(system.tree);
				int renderingNodes = LodSystem.countRenderingNodes(system.tree);
				int dirtyNodes = LodSystem.countDirty(system.tree);
				int totalNodes = LodSystem.countTotalNodes(system.tree);
				double currentQuality = system.currentQuality;
				double qualityLimit = system.qualityLimit;
				double qualityConfig = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
				String rendererName = system.renderer.getClass().getSimpleName();
				if (rendererName.endsWith("LodRenderer")) rendererName = rendererName.substring(0, rendererName.length() - "LodRenderer".length());
				callback.getReturnValue().add("[BG] LOD Renderer: " + rendererName + ", Nodes: R: " + renderingNodes + ", M: " + meshedNodes + ", D: " + dirtyNodes + ", T: " + totalNodes + ", Q: " + currentQuality + "/" + qualityLimit + "/" + qualityConfig);
				callback.getReturnValue().addAll(system.renderer.getF3MenuText());
				callback.getReturnValue().add(system.generator.f3Message());
			}
		}

	#endif
}