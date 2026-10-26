package builderb0y.bigglobe.platform.fabric.loader.api;

import java.nio.file.Path;
import java.util.Optional;

import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.loader.api.metadata.ModMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;

public class FabricLoader {

	private static final FabricLoader INSTANCE = new FabricLoader();

	public static FabricLoader getInstance() {
		return INSTANCE;
	}

	public EnvType getEnvironmentType() {
		return FMLEnvironment.dist == Dist.CLIENT ? EnvType.CLIENT : EnvType.SERVER;
	}

	public Path getGameDir() {
		return FMLPaths.GAMEDIR.get();
	}

	public Path getConfigDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	public boolean isModLoaded(String modId) {
		try {
			LoadingModList loadingList = LoadingModList.get();
			if (loadingList != null) {
				if (loadingList.getModFileById(modId) != null) {
					return true;
				}
				String altId = modId.contains("-") ? modId.replace('-', '_') : modId.replace('_', '-');
				if (loadingList.getModFileById(altId) != null) {
					return true;
				}
			}
		}
		catch (Throwable ignored) {}
		try {
			if (ModList.get() != null) {
				if (ModList.get().isLoaded(modId)) return true;
				String altId = modId.contains("-") ? modId.replace('-', '_') : modId.replace('_', '-');
				if (ModList.get().isLoaded(altId)) return true;
			}
		}
		catch (Throwable ignored) {}
		return false;
	}

	public Optional<ModContainer> getModContainer(String modId) {
		if (isModLoaded(modId)) {
			return Optional.of(new ModContainer() {
				@Override
				public ModMetadata getMetadata() {
					return new ModMetadata() {
						@Override
						public String getId() {
							return modId;
						}

						@Override
						public Version getVersion() {
							try {
								LoadingModList loadingList = LoadingModList.get();
								if (loadingList != null && loadingList.getModFileById(modId) != null) {
									return Version.parse(loadingList.getModFileById(modId).versionString());
								}
								return Version.parse(ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getVersion().toString()).orElse("1.0.0"));
							}
							catch (Exception e) {
								return new Version.Impl("1.0.0");
							}
						}

						@Override
						public String getName() {
							return modId;
						}
					};
				}
			});
		}
		return Optional.empty();
	}
}
