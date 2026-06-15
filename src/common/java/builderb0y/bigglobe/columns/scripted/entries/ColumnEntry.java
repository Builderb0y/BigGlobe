package builderb0y.bigglobe.columns.scripted.entries;

import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.compile.StagedCompileable;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

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

	public TypeInfo getTypeInfo(ColumnEntryRegistry registry) {
		return this.params.typeInfo(registry, this);
	}

	public InsnTree parseConstant(ColumnEntryRegistry registry, Data data) throws ConstantFormatException {
		return this.params.typeSpec(registry, this).parseConstant(registry.classHierarchy, data);
	}

	public abstract boolean hasFieldSetterAndFlag();

	@Override
	public void verify(ColumnEntryRegistry registry) throws DetailedException {
		super.verify(registry);
		if (this.getTypeInfo(registry).isVoid()) {
			throw new ColumnValueException("Void-typed column entry: " + UnregisteredObjectException.getID(registry.entryOf(this)));
		}
	}

	public void setupEnvironment(ColumnEntryRegistry registry, ExpressionParser parser, ExternalEnvironmentParams params) {
		Identifier selfID = UnregisteredObjectException.getID(registry.entryOf(this));
		this.implSetupEnvironment(registry, parser, params, selfID.toString());
		if (params.caller != null && params.caller.getNamespace().equals(selfID.getNamespace())) {
			int start = relativize(selfID.getPath(), params.caller.getPath());
			if (start >= 0) {
				this.implSetupEnvironment(registry, parser, params, selfID.getPath().substring(start));
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

	public void implSetupEnvironment(ColumnEntryRegistry registry, ExpressionParser parser, ExternalEnvironmentParams params, String name) {
		Holder<ColumnEntry> self = registry.entryOf(this);
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		MethodInfo getter = context.mainGetter.info;
		MethodInfo setter = params.mutable && context.mainSetter != null ? context.mainSetter.info : null;
		boolean is3D = this.params.is_3d();
		MutableScriptEnvironment environment = parser.environment.mutable();
		environment.addMethod(new MethodHandler.Named(
			getter.owner,
			name,
			"methodInvoke: " + getter,
			params.dependencyCallback(self),
			(ExpressionParser parser_, InsnTree receiver, String name_, GetMethodMode mode, InsnTree... arguments) -> {
				if (mode != GetMethodMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser_.input);
				return params.resolveColumn(parser_, name_, is3D, false, getter, setter, receiver, arguments);
			}
		));
		environment.addMethod(new MethodHandler.Named(
			ScriptedColumnLookup.TYPE,
			name,
			"methodInvoke: " + getter,
			params.dependencyCallback(self),
			(ExpressionParser parser_, InsnTree receiver, String name_, GetMethodMode mode, InsnTree... arguments) -> {
				if (mode != GetMethodMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser_.input);
				return params.resolveColumn(parser_, name_, is3D, false, getter, setter, receiver, arguments);
			}
		));
		environment.addField(new FieldHandler.Named(
			getter.owner,
			name,
			"fieldInvoke: " + getter,
			params.dependencyCallback(self),
			(ExpressionParser parser_, InsnTree receiver, String name_, GetFieldMode mode) -> {
				if (mode != GetFieldMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser_.input);
				return params.resolveColumn(parser_, name_, is3D, false, getter, setter, receiver).tree();
			}
		));
		environment.addField(new FieldHandler.Named(
			ScriptedColumnLookup.TYPE,
			name,
			"fieldInvoke: " + getter,
			params.dependencyCallback(self),
			(ExpressionParser parser_, InsnTree receiver, String name_, GetFieldMode mode) -> {
				if (mode != GetFieldMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser_.input);
				return params.resolveColumn(parser_, name_, is3D, false, getter, setter, receiver).tree();
			}
		));
	}

	public static class ColumnEntryContext {

		public int uniquifier;
		@Deprecated
		public int flagsIndex = -1;
		public String internalName;
		public @Nullable FieldCompileContext valueField;

		public MethodCompileContext mainGetter;
		public @Nullable MethodCompileContext mainSetter;
		public @Nullable MethodCompileContext preComputer;
		public @Nullable MethodCompileContext computer;

		public @Nullable ClassCompileContext borderClass;
		public @Nullable FieldCompileContext borderValueField;
		public @Nullable MethodCompileContext borderConstructor;

		public int flagsIndex() {
			if (this.flagsIndex >= 0) return this.flagsIndex;
			else throw new IllegalStateException("flagsIndex not set!");
		}
	}
}