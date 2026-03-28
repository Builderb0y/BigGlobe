package builderb0y.bigglobe.mixins;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface InGameHud_DebugHudGetter {

	@Accessor("debugOverlay")
	public abstract DebugScreenOverlay bigglobe_getDebugHud();
}