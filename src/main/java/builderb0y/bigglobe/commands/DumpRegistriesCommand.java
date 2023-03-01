package builderb0y.bigglobe.commands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;

public class DumpRegistriesCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
			CommandManager.literal(BigGlobeMod.MODID + ":dumpRegistries")
			.requires(source -> source.hasPermissionLevel(4))
			.executes(context -> {
				File root = new File(FabricLoader.getInstance().getGameDir().toFile(), "bigglobe_registry_dump");
				delete(root);
				File registryRoot = new File(root, "registries");
				File tagsRoot = new File(root, "tags");
				Comparator<Identifier> comparator = (
					Comparator
					.comparing(Identifier::getNamespace)
					.thenComparing(Identifier::getPath)
				);
				RegistryOps<JsonElement> ops = RegistryOps.of(JsonOps.INSTANCE, context.getSource().getRegistryManager());
				context
				.getSource()
				.getRegistryManager()
				.streamAllRegistries()
				.forEach(dynamicRegistryEntry -> {
					try {
						String path = identifierPath(dynamicRegistryEntry.key().getValue());
						File perRegistryRoot = new File(registryRoot, path);
						File perTagRoot = new File(tagsRoot, path);
						DynamicRegistryManager.Info info = DynamicRegistryManager.INFOS.get(dynamicRegistryEntry.key());
						if (info != null) { //dynamic registry
							for (Map.Entry<? extends RegistryKey<?>, ?> elementEntry : dynamicRegistryEntry.value().getEntrySet()) {
								JsonElement json = (JsonElement)(info.entryCodec().encodeStart(ops, elementEntry.getValue()).result().orElseThrow());
								File file = new File(perRegistryRoot, identifierPath(elementEntry.getKey().getValue()) + ".json");
								file.getParentFile().mkdirs();
								try (JsonWriter writer = new JsonWriter(new FileWriter(file))) {
									writer.setIndent("\t");
									Streams.write(json, writer);
								}
								catch (IOException exception) {
									exception.printStackTrace();
								}
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
						dynamicRegistryEntry.value().streamTagsAndEntries().forEach(pair -> {
							File file = new File(perTagRoot, identifierPath(pair.getFirst().id()) + ".txt");
							file.getParentFile().mkdirs();
							try (PrintStream stream = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
								pair.getSecond().stream().map(RegistryEntry::getKey).map(Optional::get).map(RegistryKey::getValue).sorted(comparator).forEachOrdered(stream::println);
							}
							catch (IOException exception) {
								exception.printStackTrace();
							}
						});
					}
					catch (Throwable throwable) {
						throwable.printStackTrace();
						throw AutoCodecUtil.rethrow(throwable);
					}
				});
				context.getSource().sendFeedback(Text.translatable("commands." + BigGlobeMod.MODID + ".registryDump.success"), false);
				return 1;
			})
		);
	}

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