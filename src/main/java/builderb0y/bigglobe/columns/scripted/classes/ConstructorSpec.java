package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultString;
import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.classes.BaseMethodSpec.MethodSpecDesc;
import builderb0y.bigglobe.columns.scripted.classes.OverrideTracker.TrackedField;
import builderb0y.bigglobe.columns.scripted.classes.OverrideTracker.TrackedProperty;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstructorSpec extends MemberSpec implements SetBasedMutableDependencyView {

	public final @IdentifierName @DefaultString("new") String name;
	public final @VerifyNotEmpty List<String> values;
	public final @VerifyNullable ScriptUsage code;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies = new HashSet<>();

	public ConstructorSpec(String name, List<String> values, @VerifyNullable ScriptUsage code) {
		this.name = name;
		this.values = values;
		this.code = code;
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		if (owner.isAbstract) {
			throw new CustomClassFormatException("Can't add constructor " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " to abstract class " + UnregisteredObjectException.getID(hierarchy.entryOf(owner)));
		}
		if (owner instanceof VoronoiClassSpec) {
			throw new CustomClassFormatException("Can't add constructor " + UnregisteredObjectException.getID(hierarchy.entryOf(this)) + " to voronoi class " + UnregisteredObjectException.getID(hierarchy.entryOf(owner)));
		}
		ConstructorContext context = new ConstructorContext();
		context.resolvedParameters = this.values.stream().map((String name) -> {
			TrackedField field = owner.overrideTracker.fields.get(name);
			if (field != null) {
				return new ValueSpec(name, field.declaration(), ((FieldSpec)(field.declaration().value())).field_type, false);
			}
			TrackedProperty property = owner.overrideTracker.properties.get(name);
			if (property != null) {
				if (((BasePropertySpec)(property.declaration().value())).isSettable()) {
					return new ValueSpec(name, property.declaration(), ((BasePropertySpec)(property.declaration().value())).getPropertyType(), true);
				}
				else {
					throw AutoCodecUtil.rethrow(new CustomClassFormatException("Can't assign to property " + UnregisteredObjectException.getID(property.declaration()) + " in constructor " + UnregisteredObjectException.getID(hierarchy.entryOf(this))));
				}
			}
			throw AutoCodecUtil.rethrow(new CustomClassFormatException("Could not find field named " + name + " in class " + UnregisteredObjectException.getID(hierarchy.entryOf(owner)) + " for constructor " + UnregisteredObjectException.getID(hierarchy.entryOf(this))));
		})
		.toArray(ValueSpec[]::new);
		context.descriptor = new MethodSpecDesc(
			this.name,
			Stream.concat(
				Stream.of(ScriptedColumn.INFO.type),
				Arrays.stream(context.resolvedParameters).map(ValueSpec::typeInfo)
			)
			.toList()
		);
		owner.setCompileContext(this, context);
	}

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		ConstructorContext context = owner.getCompileContext(this);
		context.methodCompileContext = owner.classCompileContext.newMethod(
			ACC_PUBLIC | ACC_STATIC,
			this.name,
			owner.getTypeInfo(),
			Stream.concat(
				Stream.of(new LazyVarInfo("column", ScriptedColumn.INFO.type)),
				Arrays
				.stream(context.resolvedParameters)
				.map((ValueSpec parameter) -> new LazyVarInfo(parameter.name, parameter.typeInfo()))
			)
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {
		ConstructorContext context = owner.getCompileContext(this);
		LazyVarInfo result = context.methodCompileContext.scopes.addVariable("$result", owner.getTypeInfo());
		store(
			result,
			newInstance(
				context.methodCompileContext.info,
				load("column", ScriptedColumn.INFO.type)
			)
		)
		.emitBytecode(context.methodCompileContext);
		for (ValueSpec parameter : context.resolvedParameters) {
			if (parameter.useSetter) {
				invokeInstance(
					load(result),
					new MethodInfo(
						ACC_PUBLIC,
						parameter.ownerTypeInfo(),
						parameter.name,
						TypeInfos.VOID,
						parameter.typeInfo()
					),
					load(parameter.name, parameter.typeInfo())
				)
				.emitBytecode(context.methodCompileContext);
			}
			else {
				putField(
					load(result),
					new FieldInfo(
						ACC_PUBLIC,
						parameter.ownerTypeInfo(),
						parameter.name,
						parameter.typeInfo()
					),
					load(parameter.name, parameter.typeInfo())
				)
				.emitBytecode(context.methodCompileContext);
			}
		}
		if (this.code != null) {
			hierarchy.registry.parseCode(
				context.methodCompileContext,
				this.code,
				load("column", ScriptedColumn.INFO.type),
				null,
				load(result),
				this,
				MemberSpec.NO_EXTRAS //other FieldSpec's can add themselves to the environment automatically.
			)
			.emitBytecode(context.methodCompileContext);
		}
		return_(load(result)).emitBytecode(context.methodCompileContext);
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addConstructor(this);
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		ConstructorContext context = owner.getCompileContext(this);
		environment.addQualifiedFunctionInvokeStatic(context.methodCompileContext.info);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return "constructor " + this.name + this.values;
	}

	public static class ConstructorContext {

		public MethodCompileContext methodCompileContext;
		public ValueSpec[] resolvedParameters;
		public MethodSpecDesc descriptor;
	}

	public static class ValueSpec {

		public final @IdentifierName String name;
		public final RegistryEntry<ElementSpec> owner, type;
		public final boolean useSetter;

		public ValueSpec(String name, RegistryEntry<ElementSpec> owner, RegistryEntry<ElementSpec> type, boolean useSetter) {
			this.name = name;
			this.owner = owner;
			this.type = type;
			this.useSetter = useSetter;
		}

		public TypeInfo typeInfo() {
			return asType(this.type).getTypeInfo();
		}

		public TypeInfo ownerTypeInfo() {
			return asType(this.owner).getTypeInfo();
		}
	}
}