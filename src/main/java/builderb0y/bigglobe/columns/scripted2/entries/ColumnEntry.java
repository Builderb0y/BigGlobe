package builderb0y.bigglobe.columns.scripted2.entries;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
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

public abstract class ColumnEntry implements CoderRegistryTyped<ColumnEntry>, DependencyView {

	public final AccessSchema params;

	public ColumnEntry(AccessSchema params) {
		this.params = params;
	}

	public abstract boolean hasFieldSetterAndFlag();

	public void verify(ColumnEntryRegistry registry) throws ColumnValueException {
		if (ElementSpec.asType(this.params.type()).getTypeInfo().isVoid()) {
			throw new ColumnValueException("Void-typed column entry: " + UnregisteredObjectException.getID(registry.entryOf(this)));
		}
	}

	public abstract void createContext(ColumnEntryRegistry registry) throws ColumnValueException;

	public abstract void compile(ColumnEntryRegistry registry) throws ColumnValueException, ScriptParsingException;

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
		RegistryEntry<ColumnEntry> self = registry.entryOf(this);
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		MethodInfo getter = context.mainGetter.info;
		MethodInfo setter = context.mainSetter != null ? context.mainSetter.info : null;
		boolean is3D = this.params.is_3d();
		environment.addMethod(getter.owner, name, new MethodHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name_, GetMethodMode mode, InsnTree... arguments) -> {
			if (mode != GetMethodMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser.input);
			if (params.dependencies != null) params.dependencies.addDependency(self);
			return params.resolveColumn(parser, name_, is3D, false, getter, setter, arguments);
		}));
		if (params.loadColumn != null) {
			environment.addFunction(name, new FunctionHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, String name_, InsnTree... arguments) -> {
				if (params.dependencies != null) params.dependencies.addDependency(self);
				return params.resolveColumn(parser, name_, is3D, false, getter, setter, arguments);
			}));
		}
		if (params.requiresNoArguments(is3D)) {
			environment.addField(getter.owner, name, new FieldHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name_, GetFieldMode mode) -> {
				if (mode != GetFieldMode.NORMAL) throw new ScriptParsingException("Nullable and receiver syntax is not supported for column value accessing", parser.input);
				if (params.dependencies != null) params.dependencies.addDependency(self);
				return params.resolveColumn(parser, name_, is3D, false, getter, setter).tree();
			}));
			if (params.loadColumn != null) {
				environment.addVariable(name, new VariableHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, String name_) -> {
					if (params.dependencies != null) params.dependencies.addDependency(self);
					return params.resolveColumn(parser, name_, is3D, false, getter, setter).tree();
				}));
			}
		}
	}

	public static enum CompileStep {
		VERIFY("verifying", ColumnEntry::verify),
		CREATE_CONTEXT("creating context", ColumnEntry::createContext),
		COMPILE("compiling", ColumnEntry::compile);

		public final String description;
		public final CompileAction action;

		CompileStep(String description, CompileAction action) {
			this.description = description;
			this.action = action;
		}

		@FunctionalInterface
		public static interface CompileAction {

			public abstract void execute(ColumnEntry columnEntry, ColumnEntryRegistry registry) throws Exception;
		}
	}

	public static class ColumnEntryContext {

		public int uniquifier;
		public String internalName;
		public MethodCompileContext mainGetter;
		public @Nullable MethodCompileContext mainSetter;
	}
}