package builderb0y.bigglobe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.LevelStorageSource;
import builderb0y.bigglobe.blockEntities.BigGlobeBlockEntityTypes;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.SoulCauldronBlock;
import builderb0y.bigglobe.brewing.BigGlobeBrewing;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.chunkgen.EmptyChunkGenerator;
import builderb0y.bigglobe.commands.BigGlobeArgumentTypes;
import builderb0y.bigglobe.commands.BigGlobeCommands;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dispensers.BigGlobeDispenserBehaviors;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.entities.BigGlobeEntityTypes;
import builderb0y.bigglobe.features.BigGlobeFeatures;
import builderb0y.bigglobe.fluids.BigGlobeFluids;
import builderb0y.bigglobe.gamerules.BigGlobeGameRules;
import builderb0y.bigglobe.hyperspace.HyperspaceCollapseTicketType;
import builderb0y.bigglobe.items.BigGlobeItems;
import builderb0y.bigglobe.loot.BigGlobeLoot;
import builderb0y.bigglobe.mixins.MinecraftServer_SessionAccess;
import builderb0y.bigglobe.mixins.SpawnRestriction_BackingMapAccess;
import builderb0y.bigglobe.networking.base.BigGlobeNetwork;
import builderb0y.bigglobe.particles.BigGlobeParticles;
import builderb0y.bigglobe.recipes.BigGlobeRecipeSerializers;
import builderb0y.bigglobe.sounds.BigGlobeSoundEvents;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.placement.BigGlobeStructurePlacementTypes;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.parsing.ExpressionParser;

public class BigGlobeMod implements ModInitializer {

	public static final String
		MODID = "bigglobe",
		MODNAME = "Big Globe";

	public static final Logger
		LOGGER = LoggerFactory.getLogger(MODNAME);
	public static final boolean
		REGEN_WORLDS = Boolean.getBoolean(MODID + ".regenWorlds"),
		MIXIN_AUDIT = Boolean.getBoolean(MODID + ".mixinAudit");
	public static final ResourceKey<WorldPreset>
		BIG_GLOBE_WORLD_PRESET_KEY = ResourceKey.create(Registries.WORLD_PRESET, modID("bigglobe"));

	public static MinecraftServer currentServer;
	public static BetterRegistry.Lookup currentRegistries;
	public static ResourceManager currentResourceManager;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing...");
		BigGlobeConfig.init();
		BigGlobeDynamicRegistries.init();

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
		BigGlobeStructurePlacementTypes.init();
		BigGlobeScriptedChunkGenerator.init();
		EmptyChunkGenerator.init();

		BigGlobeArgumentTypes.init();
		BigGlobeCommands.init();
		BigGlobeGameRules.init();
		BigGlobeNetwork.init();
		BigGlobeRecipeSerializers.init();
		ExpressionParser.clinit();
		BigGlobeParticles.init();
		HyperspaceCollapseTicketType.init();

		DistantHorizonsCompat.init();

		Map<EntityType<?>, Object> restrictions = SpawnRestriction_BackingMapAccess.bigglobe_getRestrictions();
		restrictions.putIfAbsent(EntityType.ZOGLIN, restrictions.get(EntityType.HOGLIN));
		ServerLifecycleEvents.SERVER_STARTING.register((MinecraftServer server) -> {
			currentServer = server;
			currentRegistries = new BetterRegistry.Lookup() {

				@Override
				public <T> BetterRegistry<T> getRegistry(ResourceKey<Registry<T>> key) {
					return new BetterHardCodedRegistry<>(
						RegistryVersions.getRegistry(
							server.registryAccess(),
							key
						)
					);
				}
			};
			currentResourceManager = new DelegatingResourceManager(server::getResourceManager);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> {
			currentServer = null;
			currentRegistries = null;
			currentResourceManager = null;
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
	public static <T> BetterRegistry<T> getRegistry(ResourceKey<? extends Registry<? extends T>> key) {
		return getCurrentRegistries().getRegistry((ResourceKey<Registry<T>>)(key));
	}

	public static <T> BetterRegistry<T> getSidedRegistry(ResourceKey<? extends Registry<? extends T>> key, boolean client) {
		return client ? getClientRegistry(key) : getRegistry(key);
	}

	public static <T> BetterRegistry<T> getClientRegistry(ResourceKey<? extends Registry<? extends T>> key) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			return getClientRegistry0(key);
		}
		else {
			throw new IllegalStateException("Calling getClientRegistry() on dedicated server!");
		}
	}

	@Environment(EnvType.CLIENT)
	public static <T> BetterRegistry<T> getClientRegistry0(ResourceKey<? extends Registry<? extends T>> key) {
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null) {
			Registry<T> registry = world.registryAccess().lookup(key).orElse(null);
			if (registry != null) {
				return new BetterHardCodedRegistry<>(registry);
			}
			else {
				throw new IllegalStateException("Client registry " + key.identifier() + " not available");
			}
		}
		else {
			throw new IllegalStateException("Client registries not available");
		}
	}

	public static ResourceManager getResourceManager() {
		if (currentResourceManager != null) return currentResourceManager;
		else throw new IllegalStateException("Resources not available at this time.");
	}

	public static @NotNull Identifier modID(@NotNull String path) {
		return IdentifierVersions.create(MODID, path);
	}

	public static @NotNull Identifier mcID(@NotNull String path) {
		return IdentifierVersions.vanilla(path);
	}

	public static <T> MappedRegistry<T> newRegistry(ResourceKey<Registry<T>> key) {
		return FabricRegistryBuilder.from(new MappedRegistry<>(key, Lifecycle.experimental())).buildAndRegister();
	}

	public static void regenWorlds(MinecraftServer server) {
		if (!server.getDefaultGameType().isSurvival()) {
			@SuppressWarnings({ "CastToIncompatibleInterface", "resource" })
			LevelStorageSource.LevelStorageAccess session = ((MinecraftServer_SessionAccess)(server)).bigglobe_getSession();

			MutableBoolean deletedAnything = new MutableBoolean(false);
			RegistryVersions
				.getRegistry(
					server.registryAccess(),
					Registries.LEVEL_STEM
				)
				.listElements()
				//only delete dimensions generated by big globe.
				.filter((Holder<LevelStem> options) -> options.value().generator() instanceof BigGlobeScriptedChunkGenerator)
				.peek((Holder<LevelStem> options) -> LOGGER.info("Found " + MODNAME + " dimension " + UnregisteredObjectException.getKey(options)))
				.map((Holder<LevelStem> options) -> session.getDimensionPath(
					ResourceKey.create(Registries.DIMENSION, UnregisteredObjectException.getID(options))
				))
				.flatMap((Path dimensionFolder) ->
							Stream.of("advancements", "data", "entities", "playerdata", "poi", "region", "stats")
								.map(dimensionFolder::resolve)
				)
				.forEach((Path toDelete) -> {
					if (Files.exists(toDelete)) try {
						deletedAnything.setTrue();
						PathUtils.deleteDirectory(toDelete);
						LOGGER.info("Deleted " + toDelete);
					}
					catch (Exception exception) {
						LOGGER.error("Could not delete " + toDelete, exception);
					}
				});
			if (deletedAnything.isTrue()) {
				Path voxy = session.getDimensionPath(Level.OVERWORLD).resolve("voxy");
				if (Files.exists(voxy)) try {
					PathUtils.deleteDirectory(voxy);
					LOGGER.info("Deleted " + voxy);
				}
				catch (Exception exception) {
					LOGGER.error("Could not delete " + voxy, exception);
				}
			}
		}
	}

	public static class DelegatingResourceManager implements ResourceManager {

		public final Supplier<ResourceManager> delegate;

		public DelegatingResourceManager(Supplier<ResourceManager> delegate) {
			this.delegate = delegate;
		}

		@Override
		public Set<String> getNamespaces() {
			return this.delegate.get().getNamespaces();
		}

		@Override
		public List<Resource> getResourceStack(Identifier id) {
			return this.delegate.get().getResourceStack(id);
		}

		@Override
		public Map<Identifier, Resource> listResources(String startingPath, Predicate<Identifier> allowedPathPredicate) {
			return this.delegate.get().listResources(startingPath, allowedPathPredicate);
		}

		@Override
		public Map<Identifier, List<Resource>> listResourceStacks(String startingPath, Predicate<Identifier> allowedPathPredicate) {
			return this.delegate.get().listResourceStacks(startingPath, allowedPathPredicate);
		}

		@Override
		public Stream<PackResources> listPacks() {
			return this.delegate.get().listPacks();
		}

		@Override
		public Optional<Resource> getResource(Identifier id) {
			return this.delegate.get().getResource(id);
		}
	}
}