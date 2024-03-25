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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.columns.scripted.types.VoronoiColumnValueType;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final BetterRegistry.Lookup registries;
	public final transient Map<RegistryEntry<ColumnEntry>, List<RegistryEntry<VoronoiSettings>>> voronoiOwners;
	public final transient Map<Identifier, ColumnEntryMemory> memories;
	public final transient List<ColumnEntryMemory> filteredMemories;
	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient MethodHandles.Lookup columnLookup;
	public final transient ScriptedColumn.Factory columnFactory;
	public final transient ColumnCompileContext columnContext;
	public final transient ScriptClassLoader loader;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries) throws ScriptParsingException {
		this.registries = registries;
		BetterRegistry<ColumnEntry> entries = registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY);
		BetterRegistry<VoronoiSettings> voronois = registries.getRegistry(BigGlobeDynamicRegistries.VORONOI_SETTINGS_REGISTRY_KEY);

		this.voronoiOwners = voronois.streamEntries().collect(Collectors.groupingBy((RegistryEntry<VoronoiSettings> voronoiEntry) -> {
			ColumnEntry columnEntry = voronoiEntry.value().owner().value();
			if (columnEntry instanceof VoronoiColumnEntry) {
				return voronoiEntry.value().owner();
			}
			else {
				throw new IllegalArgumentException("voronoi_settings " + UnregisteredObjectException.getID(voronoiEntry) + " is owned by column_value " + UnregisteredObjectException.getID(voronoiEntry.value().owner()) + " but this column value is not of type voronoi.");
			}
		}));

		Map<String, Identifier> voronoiCellNames = new HashMap<>(8);
		entries.streamEntries().forEach((RegistryEntry<ColumnEntry> entry) -> {
			if (entry.value() instanceof VoronoiColumnEntry voronoi) {
				String name = ((VoronoiColumnValueType)(voronoi.params.type())).name;
				Identifier id = UnregisteredObjectException.getID(entry);
				Identifier old = voronoiCellNames.putIfAbsent(name, id);
				if (old != null) {
					throw new IllegalStateException("Voronoi-typed column values " + id + " and " + old + " share the same cell name '" + name + "' (they shouldn't).");
				}
			}
		});

		this.columnContext = new ColumnCompileContext(this);
		this.memories = entries.streamEntries().collect(
			Collectors.toMap(
				UnregisteredObjectException::getID,
				this::createColumnEntryMemory
			)
		);

		Set<Identifier> voronoiDisabled = (
			voronois
			.streamEntries()
			.map(RegistryEntry::value)
			.map(VoronoiSettings::enables)
			.flatMap(Set::stream)
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
			this.columnLookup = (MethodHandles.Lookup)(this.columnClass.getDeclaredMethod("lookup").invoke(null, (Object[])(null)));
			this.columnFactory = (ScriptedColumn.Factory)(
				LambdaMetafactory.metafactory(
					this.columnLookup,
					"create",
					MethodType.methodType(ScriptedColumn.Factory.class),
					MethodType.methodType(ScriptedColumn.class, ScriptedColumn.PARAMETER_CLASSES),
						this.columnLookup.findConstructor(
						this.columnClass,
						MethodType.methodType(void.class, ScriptedColumn.PARAMETER_CLASSES)
					),
					MethodType.methodType(this.columnClass, ScriptedColumn.PARAMETER_CLASSES)
				)
				.getTarget()
				.invokeExact()
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException("Exception occurred while creating classes to hold column values.", throwable, null);
		}
	}

	public ColumnEntryMemory createColumnEntryMemory(RegistryEntry<ColumnEntry> entry) {
		ColumnEntryMemory memory = new ColumnEntryMemory(entry);
		AccessSchema accessSchema = entry.value().getAccessSchema();
		memory.putTyped(ColumnEntryMemory.TYPE_CONTEXT, this.columnContext.getTypeContext(accessSchema.type()));
		memory.putTyped(ColumnEntryMemory.ACCESS_CONTEXT, this.columnContext.getAccessContext(accessSchema));
		memory.putTyped(VoronoiColumnEntry.OPTIONS, this.voronoiOwners.getOrDefault(entry, Collections.emptyList()));
		HashSet<DataCompileContext> validOn = new HashSet<>(2);
		memory.putTyped(ColumnEntryMemory.VALID_ON, validOn);

		List<RegistryEntry<VoronoiSettings>> owners = this.voronoiOwners.get(entry);
		if (owners != null && !owners.isEmpty()) {
			owners
			.stream() //voronoi registry entries which own entry.
			.map(RegistryEntry::value) //voronoi settings which own entry.
			.map(VoronoiSettings::owner) //voronoi column entry registry entries which own entry.
			.map(RegistryEntry::value) //voronoi column entries which own entry.
			.map(ColumnEntry::getAccessSchema) //access schema associated with the owners of entry.
			.map(this.columnContext::getAccessContext) //access contexts associated with the owners of entry.
			.map(AccessContext::context) //compile contexts associated with the owners of entry.
			.filter(Objects::nonNull)
			.forEach(validOn::add);
		}
		if (validOn.isEmpty()) validOn.add(this.columnContext);
		return memory;
	}

	public static void recursiveDumpClasses(ClassCompileContext context) throws IOException {
		String baseName = context.info.getSimpleClassName();
		Files.writeString(CLASS_DUMP_DIRECTORY.resolve(baseName + "-asm.txt"), context.dump(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
		Files.write(CLASS_DUMP_DIRECTORY.resolve(baseName + ".class"), context.toByteArray(), StandardOpenOption.CREATE_NEW);
		for (ClassCompileContext innerClass : context.innerClasses) {
			recursiveDumpClasses(innerClass);
		}
	}

	public void setupInternalEnvironment(MutableScriptEnvironment environment, DataCompileContext context, ColumnValueDependencyHolder dependencies) {
		this.setupCommonEnvironment(environment);
		for (ColumnEntryMemory memory : this.memories.values()) {
			if (memory.getTyped(ColumnEntryMemory.VALID_ON).contains(context)) {
				memory.getTyped(ColumnEntryMemory.ENTRY).setupInternalEnvironment(environment, memory, context, dependencies);
			}
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(entry.getValue(), this.columnContext, environment);
		}
	}

	public void setupExternalEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		this.setupCommonEnvironment(environment);
		for (ColumnEntryMemory memory : this.filteredMemories) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironment(environment, memory, this.columnContext, params);
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(entry.getValue(), this.columnContext, environment);
		}
	}

	public void setupCommonEnvironment(MutableScriptEnvironment environment) {
		environment
		.addFieldInvoke("id",                         VoronoiDataBase.INFO.id)
		.addFieldInvoke("cell_x",                     VoronoiDataBase.INFO.get_cell_x)
		.addFieldInvoke("cell_z",                     VoronoiDataBase.INFO.get_cell_z)
		.addFieldInvoke("center_x",                   VoronoiDataBase.INFO.get_center_x)
		.addFieldInvoke("center_z",                   VoronoiDataBase.INFO.get_center_z)
		.addFieldInvoke("soft_distance_squared",      VoronoiDataBase.INFO.get_soft_distance_squared)
		.addFieldInvoke("soft_distance",              VoronoiDataBase.INFO.get_soft_distance)
		.addFieldInvoke("hard_distance_squared",      VoronoiDataBase.INFO.get_hard_distance_squared)
		.addFieldInvoke("hard_distance",              VoronoiDataBase.INFO.get_hard_distance)
		.addFieldInvoke("euclidean_distance_squared", VoronoiDataBase.INFO.get_euclidean_distance_squared)
		.addFieldInvoke("euclidean_distance",         VoronoiDataBase.INFO.get_euclidean_distance);
	}

	public static class Loading {

		public static Loading LOADING;

		public BetterRegistry.Lookup betterRegistryLookup;
		public ColumnEntryRegistry columnEntryRegistry;
		public List<DelayedCompileable> compileables;

		public Loading(BetterRegistry.Lookup betterRegistryLookup) {
			this.betterRegistryLookup = betterRegistryLookup;
		}

		static {
			ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> reset());
		}

		public static void reset() {
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry resetting: " + LOADING);
			LOADING = null;
		}

		public static void beginLoad(BetterRegistry.Lookup betterRegistryLookup) {
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry begin load: " + LOADING);
			if (LOADING == null) {
				LOADING = new Loading(betterRegistryLookup);
			}
		}

		public static Loading get() {
			if (LOADING != null) return LOADING;
			else throw new IllegalStateException("No loading context available.");
		}

		public static void endLoad(boolean successful) {
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry end load: " + LOADING);
			if (successful && LOADING != null) LOADING.compile();
		}

		public void delay(DelayedCompileable compileable) {
			if (this.columnEntryRegistry != null) {
				try {
					compileable.compile(this.columnEntryRegistry);
				}
				catch (ScriptParsingException exception) {
					throw new RuntimeException(exception);
				}
			}
			else {
				if (this.compileables == null) {
					this.compileables = new ArrayList<>(256);
				}
				this.compileables.add(compileable);
			}
		}

		public ColumnEntryRegistry getRegistry() {
			this.compile();
			return this.columnEntryRegistry;
		}

		public void compile() {
			if (this.columnEntryRegistry != null) return;
			try {
				this.columnEntryRegistry = new ColumnEntryRegistry(this.betterRegistryLookup);
			}
			catch (ScriptParsingException exception) {
				LOADING = null;
				throw new RuntimeException(exception);
			}
			if (this.compileables != null) {
				RuntimeException mainException = null;
				for (DelayedCompileable compileable : this.compileables) try {
					compileable.compile(this.columnEntryRegistry);
				}
				catch (Exception exception) {
					if (mainException == null) mainException = new RuntimeException("Some registry objects failed to compile, see below:");
					mainException.addSuppressed(exception);
				}
				this.compileables = null;
				if (mainException != null) throw mainException;
			}
		}
	}

	@UseVerifier(name = "postConstruct", in = DelayedCompileable.class, usage = MemberUsage.METHOD_IS_HANDLER, strict = false)
	public static interface DelayedCompileable {

		/** called when the {@link ColumnEntryRegistry} is constructed. */
		public abstract void compile(ColumnEntryRegistry registry) throws ScriptParsingException;

		public default boolean requiresColumns() {
			return true;
		}

		/**
		I need to add the ScriptHolder to the ColumnEntryRegistry.Loading after
		it's constructed, including after subclass constructors have run.
		this is not the intended use for verifiers, but it works.
		*/
		public static <T_Encoded> void postConstruct(VerifyContext<T_Encoded, DelayedCompileable> context) throws VerifyException {
			DelayedCompileable compileable = context.object;
			if (compileable == null) return;

			if (compileable.requiresColumns()) {
				ColumnEntryRegistry.Loading.get().delay(compileable);
			}
			else {
				try {
					compileable.compile(null);
				}
				catch (ScriptParsingException exception) {
					throw new RuntimeException(exception);
				}
			}
		}
	}
}