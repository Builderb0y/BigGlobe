package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.regex.Pattern;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;

import builderb0y.bigglobe.mixinInterfaces.SearchableDebugHud;

@Mixin(DebugHud.class)
public class DebugHud_MakeSearchable implements SearchableDebugHud {

	private Pattern bigglobe_pattern;

	@Override
	public Pattern bigglobe_getPattern() {
		return this.bigglobe_pattern;
	}

	@Override
	public void bigglobe_setPattern(Pattern pattern) {
		this.bigglobe_pattern = pattern;
	}

	@Inject(method = "getLeftText", at = @At("RETURN"))
	private void bigglobe_searchLeftText(CallbackInfoReturnable<List<String>> callback) {
		Pattern pattern = this.bigglobe_pattern;
		if (pattern != null) {
			callback.getReturnValue().replaceAll(text -> pattern.matcher(text).find() ? Formatting.GREEN + text : text);
		}
	}

	@Inject(method = "getRightText", at = @At("RETURN"))
	private void bigglobe_searchRightText(CallbackInfoReturnable<List<String>> callback) {
		Pattern pattern = this.bigglobe_pattern;
		if (pattern != null) {
			callback.getReturnValue().replaceAll(text -> pattern.matcher(text).find() ? Formatting.GREEN + text : text);
		}
	}
}