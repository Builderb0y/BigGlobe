package builderb0y.bigglobe.compat;

import net.minecraft.client.gui.screens.Screen;
import builderb0y.bigglobe.config.BigGlobeConfig;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuCompat {

	public static Screen getConfigScreen(Screen parent) {
		try {
			return AutoConfig.getConfigScreen(BigGlobeConfig.class.asSubclass(me.shedaniel.autoconfig.ConfigData.class), parent).get();
		}
		catch (Throwable ignored) {
			return null;
		}
	}
}
