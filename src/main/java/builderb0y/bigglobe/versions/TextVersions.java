package builderb0y.bigglobe.versions;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public class TextVersions {

	public static HoverEvent showText(Component text) {

		return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
	}

	public static ClickEvent suggestCommand(String command) {

		return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
	}
}
