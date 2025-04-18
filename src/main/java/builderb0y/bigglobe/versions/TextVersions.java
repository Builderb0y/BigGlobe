package builderb0y.bigglobe.versions;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

public class TextVersions {

	public static HoverEvent showText(Text text) {
		#if MC_VERSION >= MC_1_21_5
			return new HoverEvent.ShowText(text);
		#else
			return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
		#endif
	}

	public static ClickEvent suggestCommand(String command) {
		#if MC_VERSION >= MC_1_21_5
			return new ClickEvent.SuggestCommand(command);
		#else
			return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
		#endif
	}
}