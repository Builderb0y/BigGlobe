package builderb0y.bigglobe.columns.scripted;

import java.io.IOException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.Voronoi2DColumnEntry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final BetterRegistry.Lookup registries;
	public final transient Map<Identifier, ColumnEntryMemory> memories;
	public final transient List<ColumnEntryMemory> filteredMemories;
	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient ScriptedColumn.Factory columnFactory;
	public final transient ColumnCompileContext columnContext;
	public final transient ScriptClassLoader loader;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries) throws ScriptParsingException {
		this.registries = registries;
		BetterRegistry<ColumnEntry> entries = registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY);
		BetterRegistry<VoronoiSettings> voronois = registries.getRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY);
		this.memories = entries.streamEntries().collect(
			Collectors.toMap(
				UnregisteredObjectException::getID,
				ColumnEntryMemory::new
			)
		);
		this.columnContext = new ColumnCompileContext(this);
		for (Map.Entry<Identifier, ColumnEntryMemory> entry : this.memories.entrySet()) {
			entry.getValue().putTyped(ColumnEntryMemory.TYPE, this.columnContext.getSchemaType(entry.getValue().getTyped(ColumnEntryMemory.ENTRY).getAccessSchema()));
		}
		voronois.streamEntries().forEach((RegistryEntry<VoronoiSettings> voronoiEntry) -> {
			ColumnEntry columnEntry = voronoiEntry.value().owner().value();
			if (columnEntry instanceof Voronoi2DColumnEntry) {
				ColumnEntryMemory memory = this.memories.get(UnregisteredObjectException.getID(voronoiEntry.value().owner()));
				memory.addOrGet(Voronoi2DColumnEntry.OPTIONS, () -> new ArrayList<>(8)).add(voronoiEntry);
			}
			else {
				throw new IllegalArgumentException("voronoi_settings " + UnregisteredObjectException.getID(voronoiEntry) + " is owned by column_value " + UnregisteredObjectException.getID(voronoiEntry.value().owner()) + " but this column value is not of type voronoi_2d.");
			}
		});

		Set<Identifier> voronoiDisabled = (
			voronois
			.streamEntries()
			.map(RegistryEntry::value)
			.map(VoronoiSettings::enables)
			.flatMap(Arrays::stream)
			.map(UnregisteredObjectException::getID)
			.collect(Collectors.toSet())
		);
		this.filteredMemories = (
			this
			.memories
			.entrySet()
			.stream()
			.filter((Map.Entry<Identifier, ColumnEntryMemory> entry) ->
				!voronoiDisabled.contains(entry.getKey())
			)
			.map(Map.Entry::getValue)
			.toList()
		);
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).emitFieldGetterAndSetter(memory, this.columnContext);
		}
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupEnvironment(memory, this.columnContext);
		}
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).emitComputer(memory, this.columnContext);
		}
		this.columnContext.prepareForCompile();
		try {
			this.loader = new ScriptClassLoader();
			if (CLASS_DUMP_DIRECTORY != null) try {
				for (ClassCompileContext context : this.loader.loadable.values()) {
					String baseName = context.info.getSimpleClassName();
					Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), context.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
					Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), context.toByteArray(), StandardOpenOption.CREATE_NEW);
				}
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error("", exception);
			}
			this.columnClass = this.loader.defineClass(this.columnContext.mainClass).asSubclass(ScriptedColumn.class);
			MethodHandles.Lookup lookup = (MethodHandles.Lookup)(this.columnClass.getDeclaredMethod("lookup").invoke(null, (Object[])(null)));
			this.columnFactory = (ScriptedColumn.Factory)(
				LambdaMetafactory.metafactory(
					lookup,
					"create",
					MethodType.methodType(ScriptedColumn.Factory.class),
					MethodType.methodType(ScriptedColumn.class, long.class, int.class, int.class, int.class, int.class),
					lookup.findConstructor(
						this.columnClass,
						MethodType.methodType(void.class, long.class, int.class, int.class, int.class, int.class)
					),
					MethodType.methodType(this.columnClass, long.class, int.class, int.class, int.class, int.class)
				)
				.getTarget()
				.invokeExact()
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException("Exception occurred while creating classes to hold column values.", throwable, null);
		}
	}
}