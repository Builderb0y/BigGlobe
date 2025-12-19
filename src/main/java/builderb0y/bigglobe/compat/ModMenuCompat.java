package builderb0y.bigglobe.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.ConfigData;

import net.minecraft.client.gui.screen.Screen;

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
				#if MC_VERSION >= MC_1_21_11
					me.shedaniel.autoconfig.AutoConfigClient
				#else
					me.shedaniel.autoconfig.AutoConfig
				#endif
				.getConfigScreen(BigGlobeConfig.class.asSubclass(ConfigData.class), parent).get()
			);
		}
	}
}