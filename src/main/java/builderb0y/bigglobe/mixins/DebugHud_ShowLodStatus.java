package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.hud.DebugHud;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.lods.LodSystem;

@Mixin(DebugHud.class)
public class DebugHud_ShowLodStatus {

	@Inject(method = "getRightText", at = @At("RETURN"))
	private void bigglobe_showLodStatus(CallbackInfoReturnable<List<String>> callback) {
		LodSystem system = LodSystem.INSTANCE;
		if (system != null) {
			int meshedNodes = LodSystem.countMeshes(system.tree);
			int renderingNodes = LodSystem.countRenderingNodes(system.tree);
			int totalNodes = LodSystem.countTotalNodes(system.tree);
			double currentQuality = system.currentQuality;
			double qualityLimit = system.qualityLimit;
			double qualityConfig = BigGlobeConfig.INSTANCE.get().lodRendering.quality;
			callback.getReturnValue().add("[BG] LOD nodes: R: " + renderingNodes + ", M: " + meshedNodes + ", T: " + totalNodes + ", Q: " + currentQuality + "/" + qualityLimit + "/" + qualityConfig);
			system.renderer.appendTextToF3Menu(callback.getReturnValue());
		}
	}
}