package builderb0y.bigglobe.versions;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public class TextVersions {

	public static HoverEvent showText(Component text) {

		return new HoverEvent.ShowText(text);
	}

	public static ClickEvent suggestCommand(String command) {

		return new ClickEvent.SuggestCommand(command);
	}
}