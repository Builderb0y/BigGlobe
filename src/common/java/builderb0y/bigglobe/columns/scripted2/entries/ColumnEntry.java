package builderb0y.bigglobe.columns.scripted2.entries;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.compile.StagedCompileable;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.columns.scripted2.traits.WorldTraits;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

@UseCoder(name = "REGISTRY", in = ColumnEntry.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class ColumnEntry extends StagedCompileable<ColumnEntryRegistry> implements CoderRegistryTyped<ColumnEntry>, SimpleDependencyView {

	public static class Testing {

		//set to true by junit.
		public static boolean TESTING;
	}
	public static final @UnknownNullability CoderRegistry<ColumnEntry> REGISTRY = Testing.TESTING ? null : new CoderRegistry<>(BigGlobeMod.modID("column_value"));
	static {
		if (REGISTRY != null) {
			REGISTRY.registerAuto(BigGlobeMod.modID("constant"),          ConstantColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("noise"),                NoiseColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("script"),            ScriptedColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("decision_tree"), DecisionTreeColumnEntry.class);
			REGISTRY.registerAuto(BigGlobeMod.modID("voronoi"),            VoronoiColumnEntry.class);
		}
	}

	public final AccessSchema params;

	public ColumnEntry(AccessSchema params) {
		this.params = params;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.of(this.params.type());
	}

	public TypeInfo typeInfo(ColumnEntryRegistry registry) {
		return this.params.typeInfo(registry, this);
	}

	public abstract boolean hasFieldSetterAndFlag();

	@Override
	public void verify(ColumnEntryRegistry registry) throws DetailedException {
		super.verify(registry);
		if (this.params.typeInfo(registry, this).isVoid()) {
			throw new ColumnValueException("Void-typed column entry: " + UnregisteredObjectException.getID(registry.entryOf(this)));
		}
	}

	public void setupEnvironment(ColumnEntryRegistry registry, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		Identifier selfID = UnregisteredObjectException.getID(registry.entryOf(this));
		this.implSetupEnvironment(registry, environment, params, selfID.toString());
		if (params.caller != null && params.caller.getNamespace().equals(selfID.getNamespace())) {
			int start = relativize(selfID.getPath(), params.caller.getPath());
			if (start >= 0) {
				this.implSetupEnvironment(registry, environment, params, selfID.getPath().substring(start));
			}
		}
	}

	public static int relativize(String selfPath, String callerPath) {
		int start = 0;
		while (true) {
			int selfSlash = selfPath.indexOf('/', start);
			int callerSlash = callerPath.indexOf('/', start);
			if (selfSlash >= 0) {
				if (callerSlash >= 0) {
					if (selfSlash == callerSlash && selfPath.regionMatches(start, callerPath, start, selfSlash - start)) {
						start = selfSlash + 1; //a:b/c/... trying to reference a:b/c/...
					}
					else {
						return -1; //a:123/... trying to reference a:456/...
					}
				}
				else {
					return start; //a:b/123 trying to reference a:b/c/...
				}
			}
			else {
				if (callerSlash >= 0) {
					return -1; //a:b/c/... trying to reference a:b/123
				}
				else {
					return start; //a:b/123 trying to reference a:b/456
				}
			}
		}
	}

	public void implSetupEnvironment(ColumnEntryRegistry registry, MutableScriptEnvironment environment, ExternalEnvironmentParams params, String name) {
		Holder<ColumnEntry> self = registry.entryOf(this);
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		MethodInfo getter = context.mainGetter.info;
		MethodInfo setter = params.mutable && context.mainSetter != null ? context.mainSetter.info : null;
		boolean is3D = this.params.is_3d();
		environment.addMethod(getter.owner, name, new MethodHandler.Named(
			"methodInvoke: " + getter,
			(ExpressionParser parser, InsnTree receiver, String name_, GetMethodMode mode, InsnTree... arguments) -> {
				if (mode != GetMethodMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser.input);
				if (params.dependencies != null) params.dependencies.addDependency(self);
				return params.resolveColumn(parser, name_, is3D, false, getter, setter, receiver, arguments);
			}
		));
		environment.addFunction(name, new FunctionHandler.Named(
			"functionInvoke: " + getter,
			(ExpressionParser parser, String name_, InsnTree... arguments) -> {
				if (params.dependencies != null) params.dependencies.addDependency(self);
				return params.resolveColumn(parser, name_, is3D, false, getter, setter, null, arguments);
			}
		));
		if (params.requiresNoArguments(is3D, true)) {
			environment.addField(getter.owner, name, new FieldHandler.Named(
				"fieldInvoke: " + getter,
				(ExpressionParser parser, InsnTree receiver, String name_, GetFieldMode mode) -> {
					if (mode != GetFieldMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser.input);
					if (params.dependencies != null) params.dependencies.addDependency(self);
					return params.resolveColumn(parser, name_, is3D, false, getter, setter, receiver).tree();
				}
			));
		}
		if (params.requiresNoArguments(is3D, false)) {
			environment.addVariable(name, new VariableHandler.Named(
				"variableInvoke: " + getter,
				(ExpressionParser parser, String name_) -> {
					if (params.dependencies != null) params.dependencies.addDependency(self);
					return params.resolveColumn(parser, name_, is3D, false, getter, setter, null).tree();
				}
			));
		}
	}

	public static class ColumnEntryContext {

		public int uniquifier;
		@Deprecated
		public int flagsIndex = -1;
		public String internalName;
		public MethodCompileContext mainGetter;
		public @Nullable MethodCompileContext mainSetter;
		public @Nullable MethodCompileContext preComputer;
		public @Nullable MethodCompileContext computer;
		public @Nullable FieldCompileContext valueField;

		public int flagsIndex() {
			if (this.flagsIndex >= 0) return this.flagsIndex;
			else throw new IllegalStateException("flagsIndex not set!");
		}
	}
}