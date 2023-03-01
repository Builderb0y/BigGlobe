package builderb0y.bigglobe.compat;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.ConfigSerializer;

import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.config.BigGlobeConfigLoader;

public class ClothConfigCompat {

	public static Supplier<BigGlobeConfig> init() {
		try {
			return ClothCode.initCloth();
		}
		catch (LinkageError error) {
			BigGlobeConfigLoader.LOGGER.info("Failed to register ConfigSerializer. Cloth Config is probably not installed.");
			return initFallback();
		}
	}

	public static Supplier<BigGlobeConfig> initFallback() {
		return Suppliers.ofInstance(BigGlobeConfigLoader.loadAndSave());
	}

	public static class ClothCode {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public static Supplier<BigGlobeConfig> initCloth() {
			AutoConfig.register(BigGlobeConfig.class.asSubclass(ConfigData.class), ClothCode::createSerializer);
			return (Supplier)(AutoConfig.getConfigHolder(BigGlobeConfig.class.asSubclass(ConfigData.class)));
		}

		public static ConfigSerializer<ConfigData> createSerializer(Config config, Class<?> clazz) {
			return new ConfigSerializer<>() {

				@Override
				public void serialize(ConfigData config) throws SerializationException {
					try {
						BigGlobeConfigLoader.save((BigGlobeConfig)(config));
					}
					catch (Exception exception) {
						throw new SerializationException(exception);
					}
				}

				@Override
				public ConfigData deserialize() throws SerializationException {
					try {
						return (ConfigData)(BigGlobeConfigLoader.load());
					}
					catch (Exception exception) {
						throw new SerializationException(exception);
					}
				}

				@Override
				public ConfigData createDefault() {
					return (ConfigData)(new BigGlobeConfig());
				}
			};
		}
	}
}