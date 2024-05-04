package builderb0y.bigglobe.commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.UnregisteredObjectException;

import net.minecraft.registry.RegistryLoader;

public class DumpRegistriesCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal(BigGlobeMod.MODID + ":dumpRegistries")
			.requires(source -> source.hasPermissionLevel(4))
			.executes(context -> {
				try {
					dumpEverything(context);
					context.getSource().sendFeedback(() -> Text.translatable("commands." + BigGlobeMod.MODID + ".registryDump.success"), false);
				}
				catch (Throwable throwable) {
					BigGlobeMod.LOGGER.error("Error dumping registries:", throwable);
					AutoCodecUtil.rethrow(throwable);
				}
				return 1;
			})
		);
	}

	public static void dumpEverything(CommandContext<ServerCommandSource> context) {
		File root = new File(FabricLoader.getInstance().getGameDir().toFile(), "bigglobe_registry_dump");
		delete(root);
		File registryRoot   = new File(root, "registries");
		File tagsRoot       = new File(root, "tags");
		//File recipeRoot     = new File(root, "recipes");
		//File lootTablesRoot = new File(root, "loot_tables");
		RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, context.getSource().getRegistryManager());
		Map<RegistryKey<? extends Registry<?>>, Codec<?>> dynamicCodecs = new HashMap<>(RegistryLoader.DYNAMIC_REGISTRIES.size() + RegistryLoader.DIMENSION_REGISTRIES.size());
		for (RegistryLoader.Entry<?> entry : RegistryLoader.DYNAMIC_REGISTRIES) {
			dynamicCodecs.put(entry.key(), entry.elementCodec());
		}
		for (RegistryLoader.Entry<?> entry : RegistryLoader.DIMENSION_REGISTRIES) {
			dynamicCodecs.put(entry.key(), entry.elementCodec());
		}

		dumpRegistries(context, registryRoot, tagsRoot, dynamicCodecs, ops);
		//this code is disabled because mojang has not implemented shaped recipe serializing yet.
		/*
		#if MC_VERSION >= MC_1_20_2
			dumpRecipes(context, ops, recipeRoot);
			dumpLootTables(context, ops, lootTablesRoot);
		#endif
		*/
	}

	public static void dumpRegistries(CommandContext<ServerCommandSource> context, File registryRoot, File tagsRoot, Map<RegistryKey<? extends Registry<?>>, Codec<?>> dynamicCodecs, RegistryOps<JsonElement> ops) {
		context
		.getSource()
		.getRegistryManager()
		.streamAllRegistries()
		.forEach(dynamicRegistryEntry -> {
			String path = identifierPath(dynamicRegistryEntry.key().getValue());
			File perRegistryRoot = new File(registryRoot, path);
			File perTagRoot = new File(tagsRoot, path);
			@SuppressWarnings("rawtypes")
			Codec codec = dynamicCodecs.get(dynamicRegistryEntry.key());
			if (codec != null) { //dynamic registry
				for (Map.Entry<? extends RegistryKey<?>, ?> elementEntry : dynamicRegistryEntry.value().getEntrySet()) {
					codec
					.encodeStart(ops, elementEntry.getValue())
					.resultOrPartial(message -> BigGlobeMod.LOGGER.error("Error dumping " + elementEntry.getKey() + ": " + message))
					.ifPresent((Object json_) -> {
						JsonElement json = (JsonElement)(json_);
						File file = new File(perRegistryRoot, identifierPath(elementEntry.getKey().getValue()) + ".json");
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
					.getKeys()
					.stream()
					.map(RegistryKey::getValue)
					.sorted(
						Comparator
						.comparing(Identifier::getNamespace)
						.thenComparing(Identifier::getPath)
					)
					.forEachOrdered(stream::println);
				}
				catch (IOException exception) {
					exception.printStackTrace();
				}
			}
			dumpTags(dynamicRegistryEntry, perTagRoot);
		});
	}

	public static void dumpTags(DynamicRegistryManager.Entry<?> dynamicRegistryEntry, File perTagRoot) {
		Comparator<Identifier> comparator = (
			Comparator
			.comparing(Identifier::getNamespace)
			.thenComparing(Identifier::getPath)
		);
		dynamicRegistryEntry.value().streamTagsAndEntries().forEach(pair -> {
			File file = new File(perTagRoot, identifierPath(pair.getFirst().id()) + ".txt");
			file.getParentFile().mkdirs();
			try (PrintStream stream = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
				pair.getSecond().stream().map(UnregisteredObjectException::getKey).map(RegistryKey::getValue).sorted(comparator).forEachOrdered(stream::println);
			}
			catch (IOException exception) {
				BigGlobeMod.LOGGER.error("Error dumping tag #" + pair.getFirst().id() + ':', exception);
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