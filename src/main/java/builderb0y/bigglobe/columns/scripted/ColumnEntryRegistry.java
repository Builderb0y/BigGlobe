package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.TraitManager;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.util.AsyncConsumer;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.ScopeLocal;
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
	public final transient VoronoiManager voronoiManager;
	public final transient TraitManager traitManager;

	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient MethodHandles.Lookup columnLookup;
	public final transient ScriptedColumn.Factory columnFactory;
	public final transient ColumnCompileContext columnContext;
	public final transient ScriptClassLoader loader;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries, boolean client) throws ScriptParsingException {
		this.client         = client;
		this.registries     = registries;
		this.columnContext  = new ColumnCompileContext(this);
		this.voronoiManager = new VoronoiManager(this);
		this.traitManager   = new TraitManager(this);

		BetterRegistry<ColumnEntry> entries = registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY);
		Map<RegistryEntry<ColumnEntry>, Exception> exceptions = new HashMap<>(0);

		entries.streamEntries().forEach((RegistryEntry<ColumnEntry> entry) -> {
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
		.sorted(Comparator.comparing((RegistryEntry<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((RegistryEntry<ColumnEntry> entry) -> {
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
		.sorted(Comparator.comparing((RegistryEntry<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((RegistryEntry<ColumnEntry> entry) -> {
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
		this.traitManager.compile();
	}

	public static void checkExceptions(Map<RegistryEntry<ColumnEntry>, Exception> exceptions) {
		if (!exceptions.isEmpty()) {
			RuntimeException exception = new RuntimeException("Exception compiling column entries; see below.");
			for (Map.Entry<RegistryEntry<ColumnEntry>, Exception> entry : exceptions.entrySet()) {
				exception.addSuppressed(new RuntimeException("Exception compiling " + entry.getKey().getKey().map(RegistryKey::getValue).map(Identifier::toString).orElse("<unknown>"), entry.getValue()));
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

	public ColumnEntryMemory createColumnEntryMemory(RegistryEntry<ColumnEntry> entry) {
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

	public static class Loading {

		public static final boolean SKIP_STACK_TRACES = Boolean.getBoolean("bigglobe.I_understand_that_disabling_column_entry_registry_loading_stack_traces_means_that_I_am_not_allowed_to_report_bugs");
		/** the Loading instance used on the server thread when loading the world. */
		public static Loading LOADING;
		/** the Loading instance used on the client thread during synchronization of {@link ClientGeneratorParams}. */
		public static final ScopeLocal<Loading> OVERRIDE = new ScopeLocal<>();

		static {
			if (SKIP_STACK_TRACES) {
				BigGlobeMod.LOGGER.error("ColumnEntryRegistry loading stack traces are disabled. You are not allowed to report bugs.");
			}
			ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> reset());
		}

		public boolean client;
		public BetterRegistry.Lookup betterRegistryLookup;
		public ColumnEntryRegistry columnEntryRegistry;
		public List<DelayedCompileable> compileables;

		public Loading(BetterRegistry.Lookup betterRegistryLookup, boolean client) {
			this.client = client;
			this.betterRegistryLookup = betterRegistryLookup;
			this.compileables = new ArrayList<>(256);
		}

		public static void reset() {
			if (!SKIP_STACK_TRACES) BigGlobeMod.LOGGER.info("ColumnEntryRegistry resetting: " + LOADING + "; override: " + OVERRIDE.getCurrent(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
			LOADING = null;
		}

		public static void beginLoad(BetterRegistry.Lookup betterRegistryLookup) {
			if (!SKIP_STACK_TRACES) BigGlobeMod.LOGGER.info("ColumnEntryRegistry begin load: " + LOADING + "; override: " + OVERRIDE.getCurrent(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
			if (BigGlobeMod.currentRegistries == null || BigGlobeMod.currentRegistries.getClass() == betterRegistryLookup.getClass()) {
				BigGlobeMod.currentRegistries = betterRegistryLookup;
			}
			if (LOADING == null) {
				LOADING = new Loading(betterRegistryLookup, false);
			}
		}

		public static Loading get() {
			Loading override = OVERRIDE.getCurrent();
			if (override != null) return override;
			if (LOADING != null) return LOADING;
			else throw new IllegalStateException("No loading context available.");
		}

		public static void endLoad(boolean successful) {
			if (!SKIP_STACK_TRACES) BigGlobeMod.LOGGER.info("ColumnEntryRegistry end load: " + LOADING + "; override: " + OVERRIDE.getCurrent(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
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
				this.compileables.add(compileable);
			}
		}

		public ColumnEntryRegistry getRegistry() {
			if (this.columnEntryRegistry == null) {
				throw new IllegalStateException("ColumnEntryRegistry not compiled yet!");
			}
			return this.columnEntryRegistry;
		}

		public void compile() {
			if (this.columnEntryRegistry == null) try {
				this.columnEntryRegistry = new ColumnEntryRegistry(this.betterRegistryLookup, this.client);
			}
			catch (ScriptParsingException exception) {
				LOADING = null;
				throw new RuntimeException(exception);
			}
			if (!this.compileables.isEmpty()) {
				MutableObject<RuntimeException> mainException = new MutableObject<>(null);
				try (AsyncConsumer<Exception> async = new AsyncConsumer<>(BigGlobeThreadPool.mainExecutor(), (Exception exception) -> {
					if (exception != null) {
						RuntimeException main = mainException.getValue();
						if (main == null) mainException.setValue(main = new RuntimeException("Some registry objects failed to compile, see below:"));
						main.addSuppressed(exception);
					}
				})) {
					for (DelayedCompileable compileable : this.compileables) {
						async.submit(() -> {
							try {
								compileable.compile(this.columnEntryRegistry);
								return null;
							}
							catch (Exception exception) {
								return exception;
							}
						});
					}
				}
				this.compileables.clear();
				RuntimeException main = mainException.getValue();
				if (main != null) throw main;
			}
		}
	}

	@UseVerifier(name = "postConstruct", in = DelayedCompileable.class, usage = MemberUsage.METHOD_IS_HANDLER, strict = false)
	public static interface DelayedCompileable {

		/** called when the {@link ColumnEntryRegistry} is constructed. */
		public abstract void compile(ColumnEntryRegistry registry) throws ScriptParsingException;

		public default void delay() {
			Loading.get().delay(this);
		}

		/**
		I need to add the ScriptHolder to the ColumnEntryRegistry.Loading after
		it's constructed, including after subclass constructors have run.
		this is not the intended use for verifiers, but it works.
		*/
		public static <T_Encoded> void postConstruct(VerifyContext<T_Encoded, DelayedCompileable> context) throws VerifyException {
			DelayedCompileable compileable = context.object;
			if (compileable == null) return;
			compileable.delay();
		}
	}

	public static interface SimpleDelayedCompileable extends DelayedCompileable {

		public abstract void compile() throws ScriptParsingException;

		@Override
		public default void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.compile();
		}

		@Override
		public default void delay() {
			try {
				this.compile();
			}
			catch (ScriptParsingException exception) {
				throw new RuntimeException(exception);
			}
		}
	}
}