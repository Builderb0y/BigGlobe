package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public final BetterRegistry<ColumnEntry> entries;
	public final BetterRegistry<VoronoiSettings> voronois;
	public final Map<Identifier, ColumnEntryMemory> memories;
	public Class<? extends ScriptedColumn> columnClass;
	public ScriptedColumn.Factory columnFactory;

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
		try {
			this.columnClass = new ScriptClassLoader(columnContext.mainClass).defineMainClass().asSubclass(ScriptedColumn.class);
			this.columnFactory = (ScriptedColumn.Factory)(
				LambdaMetafactory.metafactory(
					MethodHandles.lookup(),
					"create",
					MethodType.methodType(ScriptedColumn.Factory.class),
					MethodType.methodType(ScriptedColumn.class, long.class, int.class, int.class, int.class, int.class, int.class),
					MethodHandles.lookup().findConstructor(
						this.columnClass,
						MethodType.methodType(void.class, long.class, int.class, int.class, int.class, int.class, int.class)
					),
					MethodType.methodType(this.columnClass, long.class, int.class, int.class, int.class, int.class, int.class)
				)
				.getTarget()
				.invokeExact()
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException("Exception occurred while creating classes to hold column values.", null);
		}
		columnContext.addFlagsFields();
	}
}