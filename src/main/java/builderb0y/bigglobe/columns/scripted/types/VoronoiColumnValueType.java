package builderb0y.bigglobe.columns.scripted.types;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.bigglobe.columns.scripted.dependencies.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.compile.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.entries.VoronoiColumnEntry;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.invokers.ArgumentedGetterSetterInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.*;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiColumnValueType implements ColumnValueType {

	public final @IdentifierName String name;
	public final @DefaultEmpty Map<@IdentifierName @UseVerifier(name = "checkNotReserved", in = VoronoiColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;

	public VoronoiColumnValueType(String name, Map<String, AccessSchema> exports) {
		this.name = name;
		this.exports = exports;
	}

	@Override
	public TypeContext createType(ColumnCompileContext context) {
		VoronoiBaseCompileContext baseContext = context.root().registry.voronoiManager.getBaseContextFor(this);
		return new TypeContext(baseContext.mainClass.info, baseContext);
	}

	@Override
	public InsnTree createConstant(Object object, ColumnCompileContext context) {
		throw new UnsupportedOperationException("Cannot create constant voronoi cell.");
	}

	@Override
	public void setupInternalEnvironment(
		MutableScriptEnvironment environment,
		TypeContext typeContext,
		DataCompileContext context,
		MutableDependencyView dependencies
	) {
		environment.addType(this.name, typeContext.type());
		VoronoiBaseCompileContext baseContext = context.root().registry.voronoiManager.getBaseContextFor(this);
		InsnTree loadHolder = (
			context.parent == baseContext
			? context.loadSelf()
			: null
		);
		if (loadHolder != null) {
			VoronoiDataBase.INFO.addAll(environment, loadHolder);
		}
		for (Map.Entry<String, AccessSchema> export : this.exports.entrySet()) {
			String name = export.getKey();
			MethodInfo getter = export.getValue().getterDescriptor(ACC_PUBLIC, "get_" + export.getKey(), baseContext);
			Runnable dependencyTrigger;
			if (dependencies != null) {
				@SuppressWarnings("unchecked")
				RegistryEntry<ColumnEntry>[] triggers = (
					context
					.root()
					.registry
					.voronoiManager
					.getOptionsFor(this)
					.stream()
					.map(RegistryEntry::value)
					.map(VoronoiSettings::exports)
					.map((Map<String, RegistryEntry<ColumnEntry>> exports) -> exports.get(export.getKey()))
					.peek(Objects::requireNonNull)
					.toArray(RegistryEntry[]::new)
				);
				dependencyTrigger = () -> {
					for (RegistryEntry<ColumnEntry> trigger : triggers) {
						dependencies.addDependency(trigger);
					}
				};
			}
			else {
				dependencyTrigger = null;
			}
			if (getter.paramTypes.length > 0) {
				if (loadHolder != null) {
					environment.addFunction(name, new FunctionHandler.Named("functionInvoke: " + getter + " for receiver " + loadHolder.describe(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (dependencyTrigger != null) dependencyTrigger.run();
						return new CastResult(invokeInstance(loadHolder, getter, castArguments), castArguments != arguments);
					}));
				}
				environment.addMethod(getter.owner, name, new MethodHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
					if (castArguments == null) return null;
					if (dependencyTrigger != null) dependencyTrigger.run();
					return new CastResult(mode.makeInvoker(parser, receiver, getter, castArguments), castArguments != arguments);
				}));
			}
			else {
				if (loadHolder != null) {
					InsnTree tree = invokeInstance(loadHolder, getter);
					environment.addVariable(name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name1) -> {
						if (dependencyTrigger != null) dependencyTrigger.run();
						return tree;
					}));
				}
				environment.addField(getter.owner, name, new FieldHandler.Named("fieldInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
					if (dependencyTrigger != null) dependencyTrigger.run();
					return mode.makeInvoker(parser, receiver, getter);
				}));
			}
		}
	}

	@Override
	public void setupExternalEnvironment(
		MutableScriptEnvironment environment,
		TypeContext typeContext,
		ColumnCompileContext context,
		ExternalEnvironmentParams params
	) {
		environment.addType(this.name, typeContext.type());
		MutableDependencyView dependencies = params.dependencies;
		List<RegistryEntry<VoronoiSettings>> options = context.registry.voronoiManager.getOptionsFor(this);
		DataCompileContext selfContext = typeContext.context();
		for (Map.Entry<String, AccessSchema> export : this.exports.entrySet()) {
			Runnable dependencyTrigger;
			if (dependencies != null) {
				@SuppressWarnings("unchecked")
				RegistryEntry<ColumnEntry>[] triggers = (
					options
					.stream()
					.map(RegistryEntry::value)
					.map(VoronoiSettings::exports)
					.map((Map<String, RegistryEntry<ColumnEntry>> exports) -> exports.get(export.getKey()))
					.peek(Objects::requireNonNull)
					.toArray(RegistryEntry[]::new)
				);
				dependencyTrigger = () -> {
					for (RegistryEntry<ColumnEntry> trigger : triggers) {
						dependencies.addDependency(trigger);
					}
				};
			}
			else {
				dependencyTrigger = null;
			}
			MethodInfo getter = export.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + export.getKey(), selfContext);
			MethodInfo setter = export.getValue().setterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "set_" + export.getKey(), selfContext);
			if (export.getValue().is_3d()) {
				if (params.mutable) {
					environment.addMethod(getter.owner, export.getKey(), (ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (dependencyTrigger != null) dependencyTrigger.run();
						return new CastResult(new ArgumentedGetterSetterInsnTree(receiver, getter, setter, castArguments[0]), castArguments != arguments);
					});
				}
				else {
					environment.addMethod(getter.owner, export.getKey(), new MethodHandler.Named("methodInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetMethodMode mode, InsnTree... arguments) -> {
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, getter, CastMode.IMPLICIT_NULL, arguments);
						if (castArguments == null) return null;
						if (dependencyTrigger != null) dependencyTrigger.run();
						return new CastResult(mode.makeInvoker(parser, receiver, getter, castArguments), castArguments != arguments);
					}));
				}
			}
			else {
				if (params.mutable) {
					environment.addField(getter.owner, export.getKey(), (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
						if (dependencyTrigger != null) dependencyTrigger.run();
						return mode.makeGetterSetter(parser, receiver, getter, setter);
					});
				}
				else {
					environment.addField(getter.owner, export.getKey(), new FieldHandler.Named("fieldInvoke: " + getter, (ExpressionParser parser, InsnTree receiver, String name1, GetFieldMode mode) -> {
						if (dependencyTrigger != null) dependencyTrigger.run();
						return mode.makeInvoker(parser, receiver, getter);
					}));
				}
			}
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.exports.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof VoronoiColumnValueType that &&
			this.name.equals(that.name) &&
			this.exports.equals(that.exports)
		);
	}

	@Override
	public String toString() {
		return "{ type: voronoi, name: " + this.name + ", exports: " + this.exports + " }";
	}
}