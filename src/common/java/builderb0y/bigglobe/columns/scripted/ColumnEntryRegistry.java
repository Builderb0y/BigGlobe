package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted2.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted2.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted2.traits.TraitManager;
import builderb0y.bigglobe.columns.scripted2.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted2.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final boolean client;
	public final BetterRegistry.Lookup registries;
	//public final transient ClassHierarchy classHierarchy;
	public final transient VoronoiManager voronoiManager;
	public final transient TraitManager traitManager;

	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient MethodHandles.Lookup columnLookup;
	public final transient ScriptedColumn.Factory columnFactory;
	public final transient ColumnCompileContext columnContext;
	public final transient ScriptClassLoader loader;
	public final transient LinkedBlockingQueue<ScriptedColumn[]> chunkReuseColumns;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries, boolean client) throws ScriptParsingException {
		this.client = client;
		this.registries = registries;
		this.columnContext = new ColumnCompileContext(this);
		//this.classHierarchy = new ClassHierarchy(this);
		this.voronoiManager = new VoronoiManager(this);
		this.traitManager = new TraitManager(this);

		BetterRegistry<ColumnEntry> entries = registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY);
		Map<Holder<ColumnEntry>, Exception> exceptions = new HashMap<>(0);

		entries.streamEntries().forEach((Holder<ColumnEntry> entry) -> {
			try {
				this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
					context.getMemories().put(
						entry.value(),
						this.createColumnEntryMemory(entry)
					);
				});
			}
			catch (Exception exception) {
				exceptions.put(entry, exception);
			}
		});
		checkExceptions(exceptions);

		entries
		.streamEntries()
		.sorted(Comparator.comparing((Holder<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((Holder<ColumnEntry> entry) -> {
			try {
				this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
					entry.value().emitFieldGetterAndSetter(context.getMemories().get(entry.value()), context);
				});
			}
			catch (Exception exception) {
				exceptions.put(entry, exception);
			}
		});
		checkExceptions(exceptions);

		entries
		.streamEntries()
		.sorted(Comparator.comparing((Holder<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((Holder<ColumnEntry> entry) -> {
			try {
				this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
					try {
						entry.value().emitComputer(context.getMemories().get(entry.value()), context);
					}
					catch (ScriptParsingException exception) {
						throw AutoCodecUtil.rethrow(exception);
					}
				});
			}
			catch (Exception exception) {
				exceptions.put(entry, exception);
			}
		});
		checkExceptions(exceptions);

		this.columnContext.prepareForCompile();
		try {
			this.loader = new ScriptClassLoader();
			this.columnClass = this.loader.defineClass(this.columnContext.mainClass, CLASS_DUMP_DIRECTORY, null).asSubclass(ScriptedColumn.class);
			this.columnLookup = (MethodHandles.Lookup)(this.columnClass.getDeclaredMethod("lookup").invoke(null, (Object[])(null)));
			this.columnFactory = (ScriptedColumn.Factory)(
				LambdaMetafactory.metafactory(
					this.columnLookup,
					"create",
					MethodType.methodType(ScriptedColumn.Factory.class),
					MethodType.methodType(ScriptedColumn.class, ScriptedColumn.CONSTRUCTOR_INFO.parameterClasses),
					this.columnLookup.findConstructor(
						this.columnClass,
						MethodType.methodType(void.class, ScriptedColumn.CONSTRUCTOR_INFO.parameterClasses)
					),
					MethodType.methodType(this.columnClass, ScriptedColumn.CONSTRUCTOR_INFO.parameterClasses)
				)
				.getTarget()
				.invokeExact()
			);
		}
		catch (Throwable throwable) {
			throw new ScriptParsingException("Exception occurred while creating classes to hold column values.", throwable, null);
		}
		this.traitManager.compile();

		int threads = Runtime.getRuntime().availableProcessors();
		this.chunkReuseColumns = new LinkedBlockingQueue<>(threads);
		for (int thread = 0; thread < threads; thread++) {
			ScriptedColumn[] columns = new ScriptedColumn[256];
			for (int index = 0; index < 256; index++) {
				columns[index] = this.columnFactory.create(new ScriptedColumn.Params(0L, 0, 0, 0, 0, ColumnUsage.GENERIC.normalHints(), null));
			}
			this.chunkReuseColumns.add(columns);
		}
	}

	public static void checkExceptions(Map<Holder<ColumnEntry>, Exception> exceptions) {
		if (!exceptions.isEmpty()) {
			RuntimeException exception = new RuntimeException("Exception compiling column entries; see below.");
			for (Map.Entry<Holder<ColumnEntry>, Exception> entry : exceptions.entrySet()) {
				exception.addSuppressed(new RuntimeException("Exception compiling " + entry.getKey().unwrapKey().map(ResourceKey::identifier).map(Identifier::toString).orElse("<unknown>"), entry.getValue()));
			}
			throw exception;
		}
	}

	public int parserFlags() {
		return this.client ? ExpressionParser.CLIENT : 0;
	}

	public int constantFlags() {
		return this.client ? AbstractConstantFactory.CLIENT : 0;
	}

	public ColumnEntryMemory createColumnEntryMemory(Holder<ColumnEntry> entry) {
		ColumnEntryMemory memory = new ColumnEntryMemory(entry);
		AccessSchema accessSchema = entry.value().getAccessSchema();
		memory.putTyped(ColumnEntryMemory.TYPE_CONTEXT, this.columnContext.getTypeContext(accessSchema.type()));
		memory.putTyped(ColumnEntryMemory.ACCESS_CONTEXT, this.columnContext.getAccessContext(accessSchema));
		return memory;
	}

	public void setupInternalEnvironment(MutableScriptEnvironment environment, DataCompileContext context, @Nullable InsnTree loadY, MutableDependencyView dependencies, @Nullable Identifier caller) {
		VoronoiDataBase.INFO.addAll(environment, null);
		this.traitManager.setupInternalEnvironment(environment, context.loadColumn(), loadY, dependencies);
		for (ColumnEntryMemory memory : context.getMemories().values()) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupInternalEnvironment(environment, memory, context, false, loadY, dependencies, caller);
		}
		if (!(context instanceof ColumnCompileContext)) {
			for (ColumnEntryMemory memory : context.root().getMemories().values()) {
				memory.getTyped(ColumnEntryMemory.ENTRY).setupInternalEnvironment(environment, memory, context, true, loadY, dependencies, caller);
			}
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupInternalEnvironment(environment, entry.getValue(), context, dependencies);
		}
	}

	public void setupExternalEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		VoronoiDataBase.INFO.addAll(environment, null);
		this.traitManager.setupExternalEnvironment(environment, params);
		for (ColumnEntryMemory memory : this.columnContext.getMemories().values()) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironment(environment, memory, this.columnContext, params);
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(environment, entry.getValue(), this.columnContext, params);
		}
	}

	public Consumer<MutableScriptEnvironment> externalEnvironmentSetterUpper(ExternalEnvironmentParams params) {
		return (MutableScriptEnvironment environment) -> this.setupExternalEnvironment(environment, params);
	}
}