package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(InGameHud.class)
public interface InGameHud_DebugHudGetter {

	@Accessor("debugHud")
	public abstract DebugHud bigglobe_getDebugHud();
}