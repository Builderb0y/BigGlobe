package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;

@Mixin(LevelRenderer.class)
public abstract class WorldRenderer_RenderHyperspaceSky {

	@Shadow
	private @Nullable ClientLevel level;
	@Shadow
	@Final
	private LevelTargetBundle targets;

	//todo: re-enable once rendering is re-written.
	/*
   	@Inject(method = "addSkyPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;", opcode = Opcodes.GETFIELD), cancellable = true)
	private void bigglobe_renderHyperspaceSky(
		FrameGraphBuilder frameGraphBuilder,
		Camera camera,
		GpuBufferSlice fog,
		CallbackInfo callback
	) {
		if (HyperspaceRenderer.INSTANCE != null && this.level.dimension() == HyperspaceConstants.WORLD_KEY) {
			FramePass framePass = frameGraphBuilder.addPass("bigglobe_hyperspace_sky");
			this.targets.main = framePass.readsAndWrites(this.targets.main); //I have no idea what this does.
			framePass.executes(HyperspaceRenderer.INSTANCE::draw);
			callback.cancel();
		}
	}
	*/
}