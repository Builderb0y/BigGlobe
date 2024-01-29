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

import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.util.TypeFormatter;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.mixinInterfaces.ColumnEntryRegistryHolder;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
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
			AccessSchema accessSchema = entry.getValue().getTyped(ColumnEntryMemory.ENTRY).getAccessSchema();
			entry.getValue().putTyped(ColumnEntryMemory.TYPE_CONTEXT, this.columnContext.getTypeContext(accessSchema.type()));
			entry.getValue().putTyped(ColumnEntryMemory.ACCESS_CONTEXT, this.columnContext.getAccessContext(accessSchema));
		}
		voronois.streamEntries().forEach((RegistryEntry<VoronoiSettings> voronoiEntry) -> {
			ColumnEntry columnEntry = voronoiEntry.value().owner().value();
			if (columnEntry instanceof VoronoiColumnEntry) {
				ColumnEntryMemory memory = this.memories.get(UnregisteredObjectException.getID(voronoiEntry.value().owner()));
				memory.addOrGet(VoronoiColumnEntry.OPTIONS, () -> new ArrayList<>(8)).add(voronoiEntry);
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
			memory.getTyped(ColumnEntryMemory.ENTRY).setupEnvironment(memory, this.columnContext, this.columnContext.loadSelf());
		}
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).emitComputer(memory, this.columnContext);
		}
		this.columnContext.prepareForCompile();
		try {
			this.loader = new ScriptClassLoader();
			if (CLASS_DUMP_DIRECTORY != null) try {
				recursiveDumpClasses(this.columnContext.mainClass);
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
					MethodType.methodType(ScriptedColumn.class, long.class, int.class, int.class, int.class, int.class, boolean.class),
					lookup.findConstructor(
						this.columnClass,
						MethodType.methodType(void.class, long.class, int.class, int.class, int.class, int.class, boolean.class)
					),
					MethodType.methodType(this.columnClass, long.class, int.class, int.class, int.class, int.class, boolean.class)
				)
				.getTarget()
				.invokeExact()
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException("Exception occurred while creating classes to hold column values.", throwable, null);
		}
	}

	public static void recursiveDumpClasses(ClassCompileContext context) throws IOException {
		String baseName = context.info.getSimpleClassName();
		Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), context.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), context.toByteArray(), StandardOpenOption.CREATE_NEW);
		for (ClassCompileContext innerClass : context.innerClasses) {
			recursiveDumpClasses(innerClass);
		}
	}

	public void setupExternalEnvironment(MutableScriptEnvironment environment, InsnTree loadColumn) {
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironment(memory, this.columnContext, environment, loadColumn);
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(entry.getValue(), this.columnContext, environment);
		}
	}

	public void setupExternalEnvironmentWithLookup(MutableScriptEnvironment environment, InsnTree loadLookup) {
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironmentWithLookup(memory, this.columnContext, environment, loadLookup);
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(entry.getValue(), this.columnContext, environment);
		}
	}

	@UseVerifier(name = "postConstruct", in = DelayedCompileable.class, usage = MemberUsage.METHOD_IS_HANDLER, strict = false)
	public static interface DelayedCompileable {

		/** called when the {@link ColumnEntryRegistry} is constructed. */
		public abstract void compile(ColumnEntryRegistry registry) throws ScriptParsingException;

		/**
		I need to add the ScriptHolder to the ColumnEntryRegistryHolder after
		it's constructed, including after subclass constructors have run.
		this is not the intended use for verifiers, but it works.
		*/
		public static <T_Encoded> void postConstruct(VerifyContext<T_Encoded, ScriptHolder<?>> context) throws VerifyException {
			ScriptHolder<?> holder = context.object;
			if (holder == null) return;

			if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
				((ColumnEntryRegistryHolder)(registryOps)).bigglobe_delayCompile(holder);
			}
			else {
				throw new VerifyException(() -> TypeFormatter.getSimpleClassName(holder.getClass()) + " was decoded using a non-registry ops: " + context.ops);
			}
		}
	}
}