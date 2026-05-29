package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagLoader.EntryWithSource;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.compile.StagedCompileable.BulkStagedCompiler;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.traits.TraitManager;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.config.BigGlobeConfig.InvalidTagHandling;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.overriders.Overrider;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.util.*;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public class ColumnEntryRegistry extends BulkStagedCompiler<ColumnEntryRegistry, ColumnEntry> {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final boolean client;
	public final BetterRegistry.Lookup registries;
	public transient ClassHierarchy classHierarchy;
	public transient TraitManager traitManager;
	public transient ColumnCompileContext columnCompileContext;
	public transient Map<ColumnEntry, Holder<ColumnEntry>> columnEntryLookup;
	public transient ScriptClassLoader loader;
	public transient Class<? extends ScriptedColumn> columnClass;
	public transient MethodHandles.Lookup columnLookup;
	public transient ScriptedColumn.Factory columnFactory;
	public transient LinkedBlockingQueue<ScriptedColumn[]> chunkGeneratorColumns;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries, boolean client) throws DetailedException {
		this.client = client;
		this.registries = registries;
		try {
			this.columnCompileContext = new ColumnCompileContext(this);
			this.classHierarchy = new ClassHierarchy(this);
			this.columnEntryLookup = (
				registries
				.getRegistry(BigGlobeDynamicRegistries.COLUMN_VALUE_REGISTRY_KEY)
				.streamEntries()
				.collect(Collectors.toMap(Holder<ColumnEntry>::value, Function.identity()))
			);

			for (CompileStep step : CompileStep.EXCEPT_FRESH) {
				this.tryProgressTo(step, null);
			}
			this.columnCompileContext.prepareForLink();
		}
		catch (Exception exception) {
			throw DetailedException.adapt(exception);
		}

		try {
			this.loader = new ScriptClassLoader();
			this.classHierarchy.link(this.loader);
			this.columnClass = this.loader.defineClass(this.columnCompileContext.clazz, CLASS_DUMP_DIRECTORY, null).asSubclass(ScriptedColumn.class);
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
			throw new ColumnValueException("Exception occurred while assembling scripted column class:", throwable);
		}
		this.traitManager.compile();

		int threads = Runtime.getRuntime().availableProcessors();
		this.chunkGeneratorColumns = new LinkedBlockingQueue<>(threads);
		for (int thread = 0; thread < threads; thread++) {
			ScriptedColumn[] columns = new ScriptedColumn[256];
			for (int index = 0; index < 256; index++) {
				columns[index] = this.columnFactory.create(new ScriptedColumn.Params(0L, 0, 0, 0, 0, ColumnUsage.GENERIC.normalHints(), null));
			}
			this.chunkGeneratorColumns.add(columns);
		}
	}

	public Holder<ColumnEntry> entryOf(ColumnEntry entry) {
		return this.columnEntryLookup.get(entry);
	}

	public Identifier idOf(ColumnEntry entry) {
		return UnregisteredObjectException.getID(this.entryOf(entry));
	}

	public int parserFlags() {
		return this.client ? ExpressionParser.CLIENT : 0;
	}

	public int constantFlags() {
		return this.client ? AbstractConstantFactory.CLIENT : 0;
	}

	public void setupEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		this.classHierarchy.setupEnvironment(environment, params);
		this.traitManager.setupEnvironment(environment, params);
		for (ColumnEntry entry : this.columnEntryLookup.keySet()) {
			entry.setupEnvironment(this, environment, params);
		}
	}

	public Consumer<MutableScriptEnvironment> environmentSetterUpper(ExternalEnvironmentParams params) {
		return (MutableScriptEnvironment environment) -> this.setupEnvironment(environment, params);
	}

	@Override
	public void tryProgressTo(CompileStep step, Void context) throws DetailedException {
		this.classHierarchy.tryProgressTo(step, context);
		super.tryProgressTo(step, context);
	}

	@Override
	public ColumnEntryRegistry getSelf() {
		return this;
	}

	@Override
	public Collection<? extends Holder<? extends ColumnEntry>> getElementsToCompileForStep(CompileStep step) {
		return this.columnEntryLookup.values();
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(Void unused) throws DetailedException {
		super.createRepresentation(unused);
		this.traitManager = new TraitManager(this);
	}

	public InsnTree parseCode(
		MethodCompileContext method,
		ScriptUsage code,
		InsnTree loadColumn,
		@Nullable InsnTree loadY,
		@Nullable InsnTree loadCustomClass,
		@Nullable Identifier caller,
		MutableDependencyView dependencies,
		Consumer<MutableScriptEnvironment> extra
	)
	throws ScriptParsingException {
		return (
			new ScriptColumnEntryParser(code, method.clazz, method, this.parserFlags())
			.configureEnvironment(JavaUtilScriptEnvironment.withoutRandom())
			.addEnvironment(MathScriptEnvironment.INSTANCE)
			.configureEnvironment(MinecraftScriptEnvironment.create())
			.addEnvironment(SymmetryScriptEnvironment.INSTANCE)
			.configureEnvironment(NbtScriptEnvironment.createMutable())
			.configureEnvironment(WoodPaletteScriptEnvironment.create(null))
			.addEnvironment(RandomScriptEnvironment.BASE)
			.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
			.addEnvironment(ColorScriptEnvironment.ENVIRONMENT)
			.configureEnvironment(ScriptedColumn.baseEnvironment(loadColumn, null, this.columnCompileContext.columnTypeInfo()))
			.configureEnvironment(this.environmentSetterUpper(
				new ExternalEnvironmentParams()
				.withColumn(loadColumn)
				.withY(loadY)
				.mutable()
				.withCaller(caller)
				.trackDependencies(dependencies)
				.withCustomClass(loadCustomClass)
			))
			.configureEnvironment((MutableScriptEnvironment environment) -> {
				if (loadY != null) environment.addVariable("y", loadY);
			})
			.configureEnvironment(extra)
			.parseEntireInput()
		);
	}

	public void setMethodCode(
		MethodCompileContext method,
		ScriptUsage code,
		InsnTree loadColumn,
		@Nullable InsnTree loadY,
		@Nullable InsnTree loadCustomClass,
		@Nullable Identifier caller,
		MutableDependencyView dependencies,
		Consumer<MutableScriptEnvironment> extra
	)
	throws ScriptParsingException {
		this.parseCode(method, code, loadColumn, loadY, loadCustomClass, caller, dependencies, extra).emitBytecode(method);
		method.endCode();
	}

	public static class Loading {

		public static final boolean LOGGING = Boolean.getBoolean("bigglobe.ColumnEntryRegistry.logging");
		/** the Loading instance used on the server thread when loading the world. */
		public static Loading LOADING;
		/**
		the Loading instance used on the client thread during
		synchronization of {@link ClientGeneratorParams}.
		*/
		public static final BetterScopedValue<Loading> OVERRIDE = new BetterScopedValue<>();

		static {
			ServerLifecycleEvents.SERVER_STOPPED.register((MinecraftServer server) -> reset());
		}

		public boolean client;
		public BetterRegistry.Lookup betterRegistryLookup;
		public ColumnEntryRegistry columnEntryRegistry;
		public List<DelayedCompileable> compileables;
		public List<DelayedEntryList<?>> tags;

		//these have to be static because tags are actually loaded before registry entries, for some reason.
		public static InvalidTagHandling invalidTagHandling;
		public static Map<Identifier, List<EntryWithSource>> invalidTags;

		public Loading(BetterRegistry.Lookup betterRegistryLookup, boolean client) {
			this.client = client;
			this.betterRegistryLookup = betterRegistryLookup;
			this.compileables = new ArrayList<>(1024);
			this.tags = new ArrayList<>(1024);
		}

		public static void reset() {
			if (LOGGING) BigGlobeMod.LOGGER.info("ColumnEntryRegistry resetting: " + LOADING + "; override: " + OVERRIDE.currentValue(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
			LOADING = null;
		}

		public static void beginLoad(BetterRegistry.Lookup betterRegistryLookup) {
			if (LOGGING) BigGlobeMod.LOGGER.info("ColumnEntryRegistry begin load: " + LOADING + "; override: " + OVERRIDE.currentValue(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
			if (BigGlobeMod.currentRegistries == null || BigGlobeMod.currentRegistries.getClass() == betterRegistryLookup.getClass()) {
				BigGlobeMod.currentRegistries = betterRegistryLookup;
			}
			if (LOADING == null) {
				LOADING = new Loading(betterRegistryLookup, false);
			}
		}

		public static Loading get() {
			Loading override = OVERRIDE.currentValue();
			if (override != null) return override;
			if (LOADING != null) return LOADING;
			else throw new IllegalStateException("No loading context available.");
		}

		public static void endLoad(boolean successful) {
			if (LOGGING) BigGlobeMod.LOGGER.info("ColumnEntryRegistry end load: " + LOADING + "; override: " + OVERRIDE.currentValue(), new Throwable("The following stack trace is NOT an error. It is debug information that is useful if you get data pack validation issues and none of your worlds load correctly."));
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

		public void addTag(DelayedEntryList<?> tag) {
			if (this.columnEntryRegistry != null) {
				tag.compile();
			}
			else {
				this.tags.add(tag);
			}
		}

		public ColumnEntryRegistry getRegistry() {
			if (this.columnEntryRegistry == null) {
				throw new IllegalStateException("ColumnEntryRegistry not compiled yet!");
			}
			return this.columnEntryRegistry;
		}

		public static boolean addInvalidTag(Identifier identifier, List<EntryWithSource> entries) {
			return switch (invalidTagHandling) {
				case VANILLA -> false;
				case FORCE_LOAD -> {
					BigGlobeMod.LOGGER.warn("Tag " + Objects.toString(identifier, "<unknown>") + " contains invalid entries: " + entries + "; forcing it to load anyway.");
					yield true;
				}
				case FORCE_ABORT -> {
					invalidTags.merge(
						identifier, new ArrayList<>(entries), (List<EntryWithSource> list1, List<EntryWithSource> list2) -> {
							ArrayList<EntryWithSource> result = new ArrayList<>(list1.size() + list2.size());
							result.addAll(list1);
							result.addAll(list2);
							return result;
						}
					);
					yield true;
				}
			};
		}

		public void compile() {
			if (!this.tags.isEmpty()) {
				for (DelayedEntryList<?> tag : this.tags) {
					tag.compile();
				}
				this.tags.clear();
			}
			if (this.columnEntryRegistry == null) try {
				this.columnEntryRegistry = new ColumnEntryRegistry(this.betterRegistryLookup, this.client);
			}
			catch (Exception exception) {
				LOADING = null;
				throw new RuntimeException(exception);
			}
			if (!this.compileables.isEmpty()) {
				MutableObject<RuntimeException> mainException = new MutableObject<>(null);
				try (AsyncConsumer<Exception> async = new AsyncConsumer<>(BigGlobeThreadPool.mainExecutor(), (Exception exception) -> {
					if (exception != null) {
						RuntimeException main = mainException.get();
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
					if (!this.client) {
						Consumer<Holder<Overrider>> action = (
							BigGlobeConfig.INSTANCE.get().dataPackDebugging.rejectUnusedOverriders
							? (Holder<Overrider> entry) -> async.submit(() -> new RuntimeException(_unusedMessage(entry)))
							: (Holder<Overrider> entry) -> BigGlobeMod.LOGGER.warn(_unusedMessage(entry))
						);
						this
						.columnEntryRegistry
						.registries
						.getRegistry(BigGlobeDynamicRegistries.OVERRIDER_REGISTRY_KEY)
						.streamEntries()
						.filter((Holder<Overrider> entry) -> entry.tags().findAny().isEmpty())
						.forEach(action);
						if (invalidTagHandling == InvalidTagHandling.FORCE_ABORT) {
							for (Map.Entry<Identifier, List<EntryWithSource>> invalidTag : invalidTags.entrySet()) {
								async.submit(() -> new RuntimeException("Tag " + Objects.toString(invalidTag.getKey(), "<unknown>") + " contains invalid entries: " + invalidTag.getValue()));
							}
						}
					}
				}
				this.compileables.clear();
				RuntimeException main = mainException.get();
				if (main != null) throw main;
			}
		}

		public static String _unusedMessage(Holder<Overrider> entry) {
			return UnregisteredObjectException.getKey(entry) + " is not in any tags. It will not be able to function unless you add it to a tag which the chunk generator uses.";
		}
	}

	@UseVerifier(name = "postConstruct", in = DelayedCompileable.class, usage = MemberUsage.METHOD_IS_HANDLER, strict = false)
	public static interface DelayedCompileable {

		/**
		called when the {@link ColumnEntryRegistry} is constructed.
		*/
		public abstract void compile(ColumnEntryRegistry registry) throws ScriptParsingException;

		public default void delay() {
			Loading.get().delay(this);
		}

		/**
		I need to add the ScriptCatcher to the ColumnEntryRegistry.Loading after
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