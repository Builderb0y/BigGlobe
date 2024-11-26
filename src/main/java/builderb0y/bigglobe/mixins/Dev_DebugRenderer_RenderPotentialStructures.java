package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;

import builderb0y.bigglobe.structures.StructureManager;
import builderb0y.bigglobe.structures.StructureManager.PotentialStructure;

@Mixin(DebugRenderer.class)
public class Dev_DebugRenderer_RenderPotentialStructures {

	@Inject(method = "render", at = @At("HEAD"))
	private void bigglobe_renderPotentialStructures(MatrixStack matrices, Immediate vertexConsumers, double cameraX, double cameraY, double cameraZ, CallbackInfo callback) {
		if (StructureManager.POTENTIAL_STRUCTURES != null) {
			List<PotentialStructure> structures = StructureManager.POTENTIAL_STRUCTURES;
			for (int index = 0, size = structures.size(); index < size; index++) {
				structures.get(index).render(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
			}
		}
	}
}