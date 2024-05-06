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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.ClientState.ClientGeneratorParams;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.CyclicDependencyAnalyzer;
import builderb0y.bigglobe.columns.scripted.dependencies.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyDepthSorter;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.bigglobe.util.AsyncConsumer;
import builderb0y.bigglobe.util.BigGlobeThreadPool;
import builderb0y.bigglobe.util.ScopeLocal;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");
	public static final Object IMAGE_SAVE_LOCK = new Object();
	public static Thread imageSaveThread;

	public final BetterRegistry.Lookup registries;
	public final VoronoiManager voronoiManager;

	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient MethodHandles.Lookup columnLookup;
	public final transient ScriptedColumn.Factory columnFactory;
	public final transient ColumnCompileContext columnContext;
	public final transient ScriptClassLoader loader;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries) throws ScriptParsingException {
		this(registries, "server");
	}

	@Hidden
	public ColumnEntryRegistry(BetterRegistry.Lookup registries, String suffix) throws ScriptParsingException {
		this.registries = registries;
		this.columnContext = new ColumnCompileContext(this);
		this.voronoiManager = new VoronoiManager(this);

		BetterRegistry<ColumnEntry> entries = registries.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY);

		entries.streamEntries().forEach((RegistryEntry<ColumnEntry> entry) -> {
			this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
				context.getMemories().put(
					entry.value(),
					this.createColumnEntryMemory(entry)
				);
			});
		});

		entries
		.streamEntries()
		.sorted(Comparator.comparing((RegistryEntry<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((RegistryEntry<ColumnEntry> entry) -> {
			this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
				entry.value().emitFieldGetterAndSetter(context.getMemories().get(entry.value()), context);
			});
		});

		entries
		.streamEntries()
		.sorted(Comparator.comparing((RegistryEntry<ColumnEntry> entry) -> entry.value() instanceof VoronoiColumnEntry)) //voronoi last.
		.forEach((RegistryEntry<ColumnEntry> entry) -> {
			this.voronoiManager.getValidOn(entry.value()).forEach((DataCompileContext context) -> {
				try {
					entry.value().emitComputer(context.getMemories().get(entry.value()), context);
				}
				catch (ScriptParsingException exception) {
					throw AutoCodecUtil.rethrow(exception);
				}
			});
		});
		this.columnContext.prepareForCompile();
		entries.streamEntries().forEach(new CyclicDependencyAnalyzer());
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging) {
			DependencyDepthSorter sorter = new DependencyDepthSorter();
			entries.streamEntries().forEach(sorter::recursiveComputeDepth);
			synchronized (IMAGE_SAVE_LOCK) {
				if (imageSaveThread != null) try {
					imageSaveThread.join();
				}
				catch (InterruptedException exception) {
					throw new RuntimeException("interrupted?", exception);
				}
				(imageSaveThread = new Thread(() -> sorter.outputResults(suffix), "Big Globe graph image saver thread")).start();
			}
		}
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

	public void setupInternalEnvironment(MutableScriptEnvironment environment, DataCompileContext context, @Nullable InsnTree loadY, MutableDependencyView dependencies) {
		VoronoiDataBase.INFO.addAll(environment, null);
		for (ColumnEntryMemory memory : context.getMemories().values()) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupInternalEnvironment(environment, memory, context, false, loadY, dependencies);
		}
		if (!(context instanceof ColumnCompileContext)) {
			for (ColumnEntryMemory memory : context.root().getMemories().values()) {
				memory.getTyped(ColumnEntryMemory.ENTRY).setupInternalEnvironment(environment, memory, context, true, loadY, dependencies);
			}
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupInternalEnvironment(environment, entry.getValue(), context, dependencies);
		}
	}

	public void setupExternalEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		VoronoiDataBase.INFO.addAll(environment, null);
		for (ColumnEntryMemory memory : this.columnContext.getMemories().values()) {
			memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironment(environment, memory, this.columnContext, params);
		}
		for (Map.Entry<ColumnValueType, TypeContext> entry : this.columnContext.columnValueTypeInfos.entrySet()) {
			entry.getKey().setupExternalEnvironment(environment, entry.getValue(), this.columnContext, params);
		}
	}

	public static class Loading {

		/** the Loading instance used on the server thread when loading the world. */
		public static Loading LOADING;
		/** the Loading instance used on the client thread during synchronization of {@link ClientGeneratorParams}. */
		public static final ScopeLocal<Loading> OVERRIDE = new ScopeLocal<>();

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
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry resetting: " + LOADING + "; override: " + OVERRIDE.getCurrent());
			LOADING = null;
		}

		public static void beginLoad(BetterRegistry.Lookup betterRegistryLookup) {
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry begin load: " + LOADING + "; override: " + OVERRIDE.getCurrent());
			if (LOADING == null) {
				LOADING = new Loading(betterRegistryLookup);
			}
		}

		public static Loading get() {
			Loading loading = OVERRIDE.getCurrent();
			if (loading != null) return loading;
			if (LOADING != null) return LOADING;
			else throw new IllegalStateException("No loading context available.");
		}

		public static void endLoad(boolean successful) {
			BigGlobeMod.LOGGER.info("ColumnEntryRegistry end load: " + LOADING + "; override: " + OVERRIDE.getCurrent());
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
				this.compileables = null;
				RuntimeException main = mainException.getValue();
				if (main != null) throw main;
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