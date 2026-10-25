package builderb0y.bigglobe.columns.scripted2;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.classes.ClassHierarchy;
import builderb0y.bigglobe.columns.scripted.classes.CustomClassFormatException;
import builderb0y.bigglobe.columns.scripted.classes.TypeSpec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.traits.TraitManager;
import builderb0y.bigglobe.columns.scripted2.entries.ColumnEntry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.util.UnregisteredObjectException;
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

public class ColumnEntryRegistry {

	public static final Path CLASS_DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpColumnValues", "bigglobe_column_values");

	public final boolean client;
	public final BetterRegistry.Lookup registries;
	public final transient ClassHierarchy classHierarchy;
	public final transient TraitManager traitManager;
	public final transient ColumnCompileContext columnCompileContext;
	public final transient Map<ColumnEntry, Holder<ColumnEntry>> columnEntryLookup;
	public final transient ScriptClassLoader loader;
	public final transient Class<? extends ScriptedColumn> columnClass;
	public final transient MethodHandles.Lookup columnLookup;
	public final transient ScriptedColumn.Factory columnFactory;

	public ColumnEntryRegistry(BetterRegistry.Lookup registries, boolean client) throws ColumnValueException, ScriptParsingException {
		this.client = client;
		this.registries = registries;
		try {
			this.classHierarchy = new ClassHierarchy(this);
		}
		catch (CustomClassFormatException exception) {
			throw new ColumnValueException(exception);
		}
		this.traitManager = null; //new TraitManager(this);
		this.columnEntryLookup = null; /*(
			registries
			.getRegistry(BigGlobeDynamicRegistries.COLUMN_ENTRY_REGISTRY_KEY)
			.streamEntries()
			.collect(Collectors.toMap(RegistryEntry<ColumnEntry>::value, Function.identity())
		);*/
		this.columnCompileContext = new ColumnCompileContext(this);

		try {
			this.classHierarchy.progressTo(TypeSpec.CompileStep.CREATE_TYPE_INFO);
			this.classHierarchy.progressTo(TypeSpec.CompileStep.VERIFY);
			this.progressTo(ColumnEntry.CompileStep.VERIFY);
			this.classHierarchy.progressTo(TypeSpec.CompileStep.CREATE_CLASS);
			this.classHierarchy.progressTo(TypeSpec.CompileStep.CREATE_MEMBERS);
			this.progressTo(ColumnEntry.CompileStep.CREATE_CONTEXT);
			this.classHierarchy.progressTo(TypeSpec.CompileStep.COMPILE_MEMBERS);
			this.progressTo(ColumnEntry.CompileStep.COMPILE);
		}
		catch (ColumnValueException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new ColumnValueException(exception);
		}

		try {
			this.loader = new ScriptClassLoader();
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
	}

	public Holder<ColumnEntry> entryOf(ColumnEntry entry) {
		return this.columnEntryLookup.get(entry);
	}

	public int parserFlags() {
		return this.client ? ExpressionParser.CLIENT : 0;
	}

	public int constantFlags() {
		return this.client ? AbstractConstantFactory.CLIENT : 0;
	}

	public void setupEnvironment(MutableScriptEnvironment environment, ExternalEnvironmentParams params, @Nullable InsnTree loadCustomClass) {
		this.classHierarchy.setupEnvironment(environment, loadCustomClass);
		for (ColumnEntry entry : this.columnEntryLookup.keySet()) {
			entry.setupEnvironment(this, environment, params);
		}
	}

	public Consumer<MutableScriptEnvironment> environmentSetterUpper(ExternalEnvironmentParams params, @Nullable InsnTree loadCustomClass) {
		return (MutableScriptEnvironment environment) -> this.setupEnvironment(environment, params, loadCustomClass);
	}

	public void progressTo(ColumnEntry.CompileStep step) throws ColumnValueException {
		ColumnValueException root = null;
		for (ColumnEntry entry : this.columnEntryLookup.keySet())
			try {
				step.action.execute(entry, this);
			}
			catch (Exception exception) {
				if (root == null) root = new ColumnValueException("Exception " + step.description);
				root.addSuppressed(new ColumnValueException("Exception " + step.description + " for " + UnregisteredObjectException.getID(this.entryOf(entry)), exception));
			}
		if (root != null) throw root;
	}

	public InsnTree parseCode(
		MethodCompileContext method,
		ScriptUsage code,
		InsnTree loadColumn,
		@Nullable InsnTree loadY,
		@Nullable InsnTree loadCustomClass,
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
				.configureEnvironment(
					this.environmentSetterUpper(
						new ExternalEnvironmentParams()
							.withColumn(loadColumn)
							.withY(loadY)
							.mutable()
							.trackDependencies(dependencies),
						loadCustomClass
					)
				)
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
		MutableDependencyView dependencies,
		Consumer<MutableScriptEnvironment> extra
	)
		throws ScriptParsingException {
		this.parseCode(method, code, loadColumn, loadY, loadCustomClass, dependencies, extra).emitBytecode(method);
		method.endCode();
	}
}