package builderb0y.bigglobe.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigData;
import net.minecraft.client.gui.screens.Screen;
import builderb0y.bigglobe.config.BigGlobeConfig;

public class ModMenuCompat implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return (Screen parent) -> {
			try {
				return ClothCode.getConfigScreen(parent);
			}
			catch (LinkageError ignored) {
				return null;
			}
		};
	}

	public static class ClothCode {

		public static Screen getConfigScreen(Screen parent) {
			return (

				AutoConfigClient

					.getConfigScreen(BigGlobeConfig.class.asSubclass(ConfigData.class), parent).get()
			);
		}
	}
}