package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import builderb0y.bigglobe.mixinInterfaces.SearchableDebugHud;

@Mixin(value = DebugScreenOverlay.class, priority = 2000)
public class DebugHud_MakeSearchable implements SearchableDebugHud {

	@Unique
	private Pattern bigglobe_pattern;

	@Override
	public Pattern bigglobe_getPattern() {
		return this.bigglobe_pattern;
	}

	@Override
	public void bigglobe_setPattern(Pattern pattern) {
		this.bigglobe_pattern = pattern;
	}

	@Inject(method = "renderLines", at = @At("HEAD"))
	private void bigglobe_searchText(GuiGraphics context, List<String> lines, boolean left, CallbackInfo callback) {
		Pattern pattern = this.bigglobe_pattern;
		if (pattern != null) {
			lines.replaceAll((String text) -> pattern.matcher(text).find() ? ChatFormatting.GREEN + text : text);
		}
	}
}