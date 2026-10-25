package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

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
			return ModList.get() != null && ModList.get().isLoaded(modId);
		}
		catch (Throwable t) {
			return false;
		}
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
