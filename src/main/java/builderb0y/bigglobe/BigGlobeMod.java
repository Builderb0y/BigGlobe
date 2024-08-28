package builderb0y.bigglobe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import org.apache.commons.io.file.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.level.storage.LevelStorage;

import builderb0y.bigglobe.blockEntities.BigGlobeBlockEntityTypes;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.SoulCauldronBlock;
import builderb0y.bigglobe.brewing.BigGlobeBrewing;
import builderb0y.bigglobe.chunkgen.*;
import builderb0y.bigglobe.commands.BigGlobeArgumentTypes;
import builderb0y.bigglobe.commands.BigGlobeCommands;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dispensers.BigGlobeDispenserBehaviors;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.loot.BigGlobeLoot;
import builderb0y.bigglobe.mixins.MinecraftServer_SessionAccess;
import builderb0y.bigglobe.mixins.SpawnRestriction_BackingMapAccess;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.particles.BigGlobeParticles;
import builderb0y.bigglobe.recipes.BigGlobeRecipeSerializers;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.parsing.ExpressionParser;

public class BigGlobeMod implements ModInitializer {

	public static final String
		MODID   = "bigglobe",
		MODNAME = "Big Globe";

	public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
	public static final boolean REGEN_WORLDS = Boolean.getBoolean(MODID + ".regenWorlds");
	public static final boolean MIXIN_AUDIT = Boolean.getBoolean(MODID + ".mixinAudit");
	public static final RegistryKey<WorldPreset> BIG_GLOBE_WORLD_PRESET_KEY = RegistryKey.of(RegistryKeyVersions.worldPreset(), modID("bigglobe"));

	public static MinecraftServer currentServer;
	public static BetterRegistry.Lookup currentRegistries;
	public static ResourceFactory currentResourceFactory;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing...");
		BigGlobeConfig.init();

		BigGlobeLoot.init();

		BigGlobeFluids.init();
		BigGlobeBlocks.init();
		BigGlobeItems.init();
		BigGlobeBlockEntityTypes.init();
		BigGlobeEntityTypes.init();
		BigGlobeSoundEvents.init();

		SoulCauldronBlock.init();
		BigGlobeDispenserBehaviors.init();
		BigGlobeBrewing.init();

		BigGlobeFeatures.init();
		BigGlobeStructures.init();
		BigGlobeScriptedChunkGenerator.init();
		EmptyChunkGenerator.init();

		BigGlobeArgumentTypes.init();
		BigGlobeCommands.init();
		BigGlobeGameRules.init();
		BigGlobeNetwork.init();
		BigGlobeRecipeSerializers.init();
		ExpressionParser.clinit();
		BigGlobeParticles.init();

		Map<EntityType<?>, Object> restrictions = SpawnRestriction_BackingMapAccess.bigglobe_getRestrictions();
		restrictions.putIfAbsent(EntityType.ZOGLIN, restrictions.get(EntityType.HOGLIN));
		ServerLifecycleEvents.SERVER_STARTING.register((MinecraftServer server) -> {
			currentServer = server;
			currentRegistries = new BetterRegistry.Lookup() {

				@Override
				public <T> BetterRegistry<T> getRegistry(RegistryKey<Registry<T>> key) {
					return new BetterHardCodedRegistry<>(server.getRegistryManager().get(key));
				}
			};
			currentResourceFactory = (Identifier identifier) -> server.getResourceManager().getResource(identifier);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
			currentServer = null;
			currentRegistries = null;
			currentResourceFactory = null;
		});
		if (REGEN_WORLDS) {
			LOGGER.error("################################################################");
			LOGGER.error("Warning! -D" + MODID + ".regenWorlds is set to true in your java arguments!");
			LOGGER.error("THIS WILL DELETE EVERYTHING IN YOUR WORLDS!");
			LOGGER.error("If you care about your worlds, CLOSE THE GAME NOW AND REMOVE THIS FROM YOUR JAVA ARGUMENTS!");
			LOGGER.error("################################################################");
			ServerLifecycleEvents.SERVER_STARTING.register(BigGlobeMod::regenWorlds);
		}
		LOGGER.info("Done initializing.");

		if (MIXIN_AUDIT) {
			MixinEnvironment.getCurrentEnvironment().audit();
			LOGGER.info("Audit complete.");
		}
	}

	public static MinecraftServer getCurrentServer() {
		if (currentServer != null) return currentServer;
		else throw new IllegalStateException("No server is running.");
	}

	public static BetterRegistry.Lookup getCurrentRegistries() {
		BetterRegistry.Lookup registries = currentRegistries;
		if (registries != null) return registries;
		else throw new IllegalStateException("Registries not available at this time.");
	}

	@SuppressWarnings("unchecked")
	public static <T> BetterRegistry<T> getRegistry(RegistryKey<? extends Registry<T>> key) {
		return getCurrentRegistries().getRegistry((RegistryKey<Registry<T>>)(key));
	}

	public static ResourceFactory getResourceFactory() {
		if (currentResourceFactory != null) return currentResourceFactory;
		else throw new IllegalStateException("Resources not available at this time.");
	}

	public static @NotNull Identifier modID(@NotNull String path) {
		return IdentifierVersions.create(MODID, path);
	}

	public static @NotNull Identifier mcID(@NotNull String path) {
		return IdentifierVersions.vanilla(path);
	}

	public static <T> SimpleRegistry<T> newRegistry(RegistryKey<Registry<T>> key) {
		return FabricRegistryBuilder.from(new SimpleRegistry<>(key, Lifecycle.experimental())).buildAndRegister();
	}

	public static void regenWorlds(MinecraftServer server) {
		if (!server.getDefaultGameMode().isSurvivalLike()) {
			@SuppressWarnings({ "CastToIncompatibleInterface", "resource" })
			LevelStorage.Session session = ((MinecraftServer_SessionAccess)(server)).bigglobe_getSession();
			server
			.getRegistryManager()
			.get(RegistryKeyVersions.dimension())
			.streamEntries()
			//only delete dimensions generated by big globe.
			.filter((RegistryEntry<DimensionOptions> options) -> options.value().chunkGenerator() instanceof BigGlobeScriptedChunkGenerator)
			.peek((RegistryEntry<DimensionOptions> options) -> LOGGER.info("Found " + MODNAME + " dimension " + UnregisteredObjectException.getKey(options)))
			.map((RegistryEntry<DimensionOptions> options) -> session.getWorldDirectory(
				RegistryKey.of(RegistryKeyVersions.world(), UnregisteredObjectException.getID(options))
			))
			.flatMap((Path dimensionFolder) ->
				Stream.of("advancements", "data", "entities", "playerdata", "poi", "region", "stats", "voxy")
				.map(dimensionFolder::resolve)
			)
			.forEach((Path toDelete) -> {
				if (Files.exists(toDelete)) try {
					PathUtils.deleteDirectory(toDelete);
					LOGGER.info("Deleted " + toDelete);
				}
				catch (Exception exception) {
					LOGGER.error("Could not delete " + toDelete, exception);
				}
			});
		}
	}
}