package builderb0y.bigglobe.mixins;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.config.BigGlobeMixinConfig;

public class BigGlobeMixinPlugin implements IMixinConfigPlugin {

	public static final Logger LOGGER = LoggerFactory.getLogger("Big Globe/Mixins");

	@Override
	public void onLoad(String mixinPackage) {
		BigGlobeMixinConfig.init();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	public static Version version(String version) {
		try {
			return Version.parse(version);
		}
		catch (VersionParsingException exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}

	public static boolean checkMod(String mixinName, String modName) {
		if (FabricLoader.getInstance().isModLoaded(modName)) {
			LOGGER.info("Applying mixin " + mixinName + " because required mod " + modName + " is present.");
			return true;
		}
		else {
			LOGGER.info("Not applying mixin " + mixinName + " because required mod " + modName + " is absent.");
			return false;
		}
	}

	public static boolean checkMod(String mixinName, String modName, Predicate<Version> versionPredicate) {
		if (
			FabricLoader
				.getInstance()
				.getModContainer(modName)
				.filter((ModContainer container) -> versionPredicate.test(container.getMetadata().getVersion()))
				.isPresent()
		) {
			LOGGER.info("Applying mixin " + mixinName + " because a known version of required mod " + modName + " is present.");
			return true;
		}
		else {
			LOGGER.info("Not applying mixin " + mixinName + " because a known version of required mod " + modName + " is absent.");
			return false;
		}
	}

	public static boolean checkNoMod(String mixinName, String modName) {
		if (FabricLoader.getInstance().isModLoaded(modName)) {
			LOGGER.info("Not applying mixin " + mixinName + " because incompatible mod " + modName + " is present.");
			return false;
		}
		else {
			LOGGER.info("Applying mixin " + mixinName + " because incompatible mod " + modName + " is absent.");
			return true;
		}
	}

	public static boolean checkNoMod(String mixinName, String modName, Predicate<Version> versionPredicate) {
		if (
			FabricLoader
			.getInstance()
			.getModContainer(modName)
			.filter((ModContainer container) -> versionPredicate.test(container.getMetadata().getVersion()))
			.isPresent()
		) {
			LOGGER.info("Not applying mixin " + mixinName + " because a known version of incompatible mod " + modName + " is present.");
			return false;
		}
		else {
			LOGGER.info("Applying mixin " + mixinName + " because a known version of incompatible mod " + modName + " is absent.");
			return true;
		}
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!BigGlobeMixinConfig.INSTANCE.knowsAbout(mixinClassName)) {
			String message = "Mixin " + mixinClassName + " does not specify its configurability!";
			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				throw new IllegalStateException(message);
			}
			else {
				LOGGER.warn(message);
			}
		}
		return switch (mixinClassName) {
			case "builderb0y.bigglobe.mixins.BigGlobeConfig_ImplementConfigData" -> {
				yield checkMod(mixinClassName, "cloth-config");
			}
			case
				"builderb0y.bigglobe.mixins.MobSpawnerLogic_SpawnLightning",
				"builderb0y.bigglobe.mixins.Camera_HandleSoulLavaSubmersion"
			-> {
				yield BigGlobeMixinConfig.INSTANCE.isEnabled(mixinClassName) && checkNoMod(mixinClassName, "connector");
			}
			case
				"builderb0y.bigglobe.mixins.Blocks_RegisterVanillaBlocksEarly",
				"builderb0y.bigglobe.mixins.Items_PlaceableFlint",
				"builderb0y.bigglobe.mixins.Items_PlaceableSticks"
			-> {
				yield BigGlobeMixinConfig.INSTANCE.isEnabled("builderb0y.bigglobe.mixins.PlaceableFlintAndSticks");
			}
			case BigGlobeMixinConfig.CHUNK_PYRAMID_MIXIN_CLASS_NAME -> {
				yield BigGlobeMixinConfig.INSTANCE.featureChunkRadius > 0;
			}
			default -> {
				yield BigGlobeMixinConfig.INSTANCE.isEnabled(mixinClassName);
			}
		};
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}