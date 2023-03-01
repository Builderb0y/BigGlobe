package builderb0y.bigglobe.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;

/** I don't like vignette. */
@Mixin(InGameHud.class)
@Environment(EnvType.CLIENT)
public class Dev_InGameHud_DisableVignette {

	@Overwrite
	private void renderVignetteOverlay(Entity entity) {}
}