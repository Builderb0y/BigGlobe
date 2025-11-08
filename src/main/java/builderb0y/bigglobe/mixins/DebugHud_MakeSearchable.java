package builderb0y.bigglobe.mixins;

import java.util.List;
import java.util.regex.Pattern;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;

import builderb0y.bigglobe.mixinInterfaces.SearchableDebugHud;

@Mixin(value = DebugHud.class, priority = 2000)
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

	#if MC_VERSION >= MC_1_21_9

		@Inject(method = "drawText", at = @At("HEAD"))
		private void bigglobe_searchText(DrawContext context, List<String> lines, boolean left, CallbackInfo callback) {
			Pattern pattern = this.bigglobe_pattern;
			if (pattern != null) {
				lines.replaceAll((String text) -> pattern.matcher(text).find() ? Formatting.GREEN + text : text);
			}
		}

	#else

		@Inject(method = "getLeftText", at = @At("RETURN"))
		private void bigglobe_searchLeftText(CallbackInfoReturnable<List<String>> callback) {
			Pattern pattern = this.bigglobe_pattern;
			if (pattern != null) {
				callback.getReturnValue().replaceAll((String text) -> pattern.matcher(text).find() ? Formatting.GREEN + text : text);
			}
		}

		@Inject(method = "getRightText", at = @At("RETURN"))
		private void bigglobe_searchRightText(CallbackInfoReturnable<List<String>> callback) {
			Pattern pattern = this.bigglobe_pattern;
			if (pattern != null) {
				callback.getReturnValue().replaceAll((String text) -> pattern.matcher(text).find() ? Formatting.GREEN + text : text);
			}
		}

	#endif
}