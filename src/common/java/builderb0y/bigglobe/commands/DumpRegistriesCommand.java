package builderb0y.bigglobe.commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryAccess.RegistryEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.CommandVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class DumpRegistriesCommand {

	public static final Comparator<Identifier> IDENTIFIER_COMPARATOR = (
		Comparator
		.comparing(Identifier::getNamespace)
		.thenComparing(Identifier::getPath)
	);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal(BigGlobeMod.MODID + ":dumpRegistries")
			.requires(CommandVersions.levelPredicate(4).or((CommandSourceStack source) -> source.getServer() != null && source.getServer().isSingleplayer()))
			.executes((CommandContext<CommandSourceStack> context) -> tryDump(context, false))
			.then(
				Commands
				.literal("full")
				.executes((CommandContext<CommandSourceStack> context) -> tryDump(context, true))
			)
		);
	}

	public static int tryDump(CommandContext<CommandSourceStack> context, boolean full) {
		try {
			dumpEverything(context, full);
			context.getSource().sendSuccess(() -> Component.translatable("commands.bigglobe.registryDump.success"), false);
			return 1;
		}
		catch (Throwable throwable) {
			BigGlobeMod.LOGGER.error("Error dumping registries:", throwable);
			context.getSource().sendFailure(Component.translatable("commands.bigglobe.registryDump.failure"));
			return 0;
		}
	}

	public static void dumpEverything(CommandContext<CommandSourceStack> context, boolean full) {
		File root = new File(FabricLoader.getInstance().getGameDir().toFile(), "bigglobe_registry_dump");
		delete(root);
		File registryRoot = new File(root, "registries");
		File tagsRoot = new File(root, "tags");
		//File recipeRoot     = new File(root, "recipes");
		//File lootTablesRoot = new File(root, "loot_tables");
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, context.getSource().registryAccess());
		Map<ResourceKey<? extends Registry<?>>, Codec<?>> dynamicCodecs;
		if (full) {
			dynamicCodecs = new Object2ObjectOpenHashMap<>(RegistryDataLoader.WORLDGEN_REGISTRIES.size() + RegistryDataLoader.DIMENSION_REGISTRIES.size());
			for (RegistryDataLoader.RegistryData<?> entry : RegistryDataLoader.WORLDGEN_REGISTRIES) {
				dynamicCodecs.put(entry.key(), entry.elementCodec());
			}
			for (RegistryDataLoader.RegistryData<?> entry : RegistryDataLoader.DIMENSION_REGISTRIES) {
				dynamicCodecs.put(entry.key(), entry.elementCodec());
			}
		}
		else {
			dynamicCodecs = Collections.emptyMap();
		}

		dumpRegistries(context, registryRoot, tagsRoot, dynamicCodecs, ops, full);
		//this code is disabled because mojang has not implemented shaped recipe serializing yet.
		/*
		#if MC_VERSION >= MC_1_20_2
			dumpRecipes(context, ops, recipeRoot);
			dumpLootTables(context, ops, lootTablesRoot);
		#endif
		*/
	}

	public static void dumpRegistries(
		CommandContext<CommandSourceStack> context,
		File registryRoot,
		File tagsRoot,
		Map<ResourceKey<? extends Registry<?>>, Codec<?>> dynamicCodecs,
		RegistryOps<JsonElement> ops,
		boolean full
	) {
		context
		.getSource()
		.registryAccess()
		.registries()
		.forEach((RegistryEntry<?> dynamicRegistryEntry) -> {
			String path = identifierPath(dynamicRegistryEntry.key().identifier());
			File perRegistryRoot = new File(registryRoot, path);
			File perTagRoot = new File(tagsRoot, path);
			@SuppressWarnings("rawtypes")
			Codec codec = dynamicCodecs.get(dynamicRegistryEntry.key());
			if (codec != null) { //dynamic registry
				for (Map.Entry<? extends ResourceKey<?>, ?> elementEntry : dynamicRegistryEntry.value().entrySet()) {
					codec
					.encodeStart(ops, elementEntry.getValue())
					.resultOrPartial(message -> BigGlobeMod.LOGGER.error("Error dumping " + elementEntry.getKey() + ": " + message))
					.ifPresent((Object json_) -> {
						JsonElement json = (JsonElement)(json_);
						File file = new File(perRegistryRoot, identifierPath(elementEntry.getKey().identifier()) + ".json");
						file.getParentFile().mkdirs();
						try (JsonWriter writer = new JsonWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
							writer.setIndent("\t");
							Streams.write(json, writer);
						}
						catch (IOException exception) {
							BigGlobeMod.LOGGER.error("Error dumping " + elementEntry.getKey() + ':', exception);
						}
					});
				}
			}
			else { //static registry
				perRegistryRoot.getParentFile().mkdirs();
				try (PrintStream stream = new PrintStream(new FileOutputStream(perRegistryRoot.getPath() + ".txt"), false, StandardCharsets.UTF_8)) {
					dynamicRegistryEntry
					.value()
					.registryKeySet()
					.stream()
					.map(ResourceKey::identifier)
					.sorted(
						Comparator
						.comparing(Identifier::getNamespace)
						.thenComparing(Identifier::getPath)
					)
					.forEachOrdered(stream::println);
				}
				catch (IOException exception) {
					BigGlobeMod.LOGGER.error("Error dumping " + dynamicRegistryEntry.key() + ':', exception);
				}
			}
			if (full) dumpTagsFull(dynamicRegistryEntry, perTagRoot);
			else dumpTagsPartial(dynamicRegistryEntry, perTagRoot);
		});
	}

	public static void dumpTagsPartial(RegistryAccess.RegistryEntry<?> dynamicRegistryEntry, File perTagRoot) {
		try (PrintStream stream = new PrintStream(new FileOutputStream(perTagRoot.getPath() + ".txt"), false, StandardCharsets.UTF_8)) {
			RegistryVersions
			.streamTags(dynamicRegistryEntry.value())
			.map(UnregisteredObjectException::getTagKey)
			.map(TagKey::location)
			.sorted(IDENTIFIER_COMPARATOR)
			.forEachOrdered(stream::println);
		}
		catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public static void dumpTagsFull(RegistryAccess.RegistryEntry<?> dynamicRegistryEntry, File perTagRoot) {
		RegistryVersions.streamTags(dynamicRegistryEntry.value()).forEach((HolderSet<?> list) -> {
			File file = new File(perTagRoot, identifierPath(UnregisteredObjectException.getTagKey(list).location()) + ".txt");
			file.getParentFile().mkdirs();
			try (PrintStream stream = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
				list.stream().map(UnregisteredObjectException::getKey).map(ResourceKey::identifier).sorted(IDENTIFIER_COMPARATOR).forEachOrdered(stream::println);
			}
			catch (IOException exception) {
				BigGlobeMod.LOGGER.error("Error dumping tag #" + UnregisteredObjectException.getTagKey(list).location() + ':', exception);
			}
		});
	}

	//this code is disabled because mojang has not implemented shaped recipe serializing yet.
	/*
	#if MC_VERSION >= MC_1_20_2
		public static void dumpRecipes(CommandContext<ServerCommandSource> context, RegistryOps<JsonElement> ops, File recipeRoot) {
			context
			.getSource()
			.getServer()
			.getRecipeManager()
			.values()
			.forEach((RecipeEntry<?> entry) -> {
				@SuppressWarnings("unchecked")
				Codec<Recipe<?>> codec = (Codec<Recipe<?>>)(entry.value().getSerializer().codec());
				codec
				.encodeStart(ops, entry.value())
				.resultOrPartial(message -> BigGlobeMod.LOGGER.error("Error dumping recipe " + entry.id() + ": " + message))
				.ifPresent((JsonElement json) -> {
					File file = new File(recipeRoot, identifierPath(entry.id()) + ".json");
					file.getParentFile().mkdirs();
					try (JsonWriter writer = new JsonWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
						writer.setIndent("\t");
						Streams.write(json, writer);
					}
					catch (IOException exception) {
						BigGlobeMod.LOGGER.error("Error dumping recipe " + entry.id() + ':', exception);
					}
				});
			});
		}

		public static void dumpLootTables(CommandContext<ServerCommandSource> context, RegistryOps<JsonElement> ops, File lootTablesRoot) {
			(
				(LootManager_KeyToValueGetter)(
					context
					.getSource()
					.getServer()
					.getLootManager()
				)
			)
			.bigglobe_getBackingMap()
			.entrySet()
			.stream()
			.filter(entry -> entry.getKey().type() == LootDataType.LOOT_TABLES)
			.forEach((Map.Entry<LootDataKey<?>, ?> entry) -> {
				Identifier identifier = entry.getKey().id();
				LootTable lootTable = (LootTable)(entry.getValue());
				LootTable
				.CODEC
				.encodeStart(ops, lootTable)
				.resultOrPartial(message -> BigGlobeMod.LOGGER.error("Error dumping loot table " + entry.getKey().id() + ": " + message))
				.ifPresent((JsonElement json) -> {
					File file = new File(lootTablesRoot, identifierPath(identifier) + ".json");
					file.getParentFile().mkdirs();
					try (JsonWriter writer = new JsonWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
						writer.setIndent("\t");
						Streams.write(json, writer);
					}
					catch (IOException exception) {
						BigGlobeMod.LOGGER.error("Error dumping loot table " + identifier + ':', exception);
					}
				});
			});
		}
	#endif
	*/

	public static String identifierPath(Identifier identifier) {
		return identifier.getNamespace() + File.separatorChar + identifier.getPath().replace('/', File.separatorChar);
	}

	public static void delete(File file) {
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				delete(child);
			}
		}
		file.delete();
	}
}