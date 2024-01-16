package builderb0y.bigglobe.columns.scripted;

import java.io.IOException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.DataCompileContext.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.Voronoi2DColumnEntry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final BetterRegistry<ColumnEntry> entries;
	public final BetterRegistry<VoronoiSettings> voronois;
	public final transient Map<Identifier, ColumnEntryMemory> memories;
	public transient Class<? extends ScriptedColumn> columnClass;
	public transient ScriptedColumn.Factory columnFactory;

	public ColumnEntryRegistry(BetterRegistry<ColumnEntry> entries, BetterRegistry<VoronoiSettings> voronois) throws ScriptParsingException {
		this.entries  = entries;
		this.voronois = voronois;
		this.memories = entries.streamEntries().collect(
			Collectors.toMap(
				UnregisteredObjectException::getID,
				ColumnEntryMemory::new
			)
		);
		DataCompileContext columnContext = new ColumnCompileContext(this);
		for (Map.Entry<Identifier, ColumnEntryMemory> entry : this.memories.entrySet()) {
			entry.getValue().putTyped(ColumnEntryMemory.TYPE, columnContext.getSchemaType(entry.getValue().getTyped(ColumnEntryMemory.ENTRY).getAccessSchema()));
		}
		voronois.streamEntries().forEach((RegistryEntry<VoronoiSettings> voronoiEntry) -> {
			ColumnEntry columnEntry = voronoiEntry.value().owner().value();
			if (columnEntry instanceof Voronoi2DColumnEntry voronoiColumnEntry) {
				voronoiColumnEntry.options.add(voronoiEntry);
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
		List<ColumnEntryMemory> filteredMemories = (
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
		for (ColumnEntryMemory memory : filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).emitFieldGetterAndSetter(memory, columnContext);
		}
		for (ColumnEntryMemory memory : filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupEnvironment(memory, columnContext);
		}
		for (ColumnEntryMemory memory : filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).emitComputer(memory, columnContext);
		}
		columnContext.prepareForCompile();
		try {
			ScriptClassLoader loader = new ScriptClassLoader(columnContext.mainClass);
			if (CLASS_DUMP_DIRECTORY != null) try {
				for (ClassCompileContext context : loader.loadable.values()) {
					String baseName = context.info.getSimpleClassName();
					Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), context.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
					Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), context.toByteArray(), StandardOpenOption.CREATE_NEW);
				}
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error("", exception);
			}
			this.columnClass = loader.defineMainClass().asSubclass(ScriptedColumn.class);
			this.columnFactory = (ScriptedColumn.Factory)(
				LambdaMetafactory.metafactory(
					MethodHandles.lookup(),
					"create",
					MethodType.methodType(ScriptedColumn.Factory.class),
					MethodType.methodType(ScriptedColumn.class, long.class, int.class, int.class, int.class, int.class),
					MethodHandles.lookup().findConstructor(
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