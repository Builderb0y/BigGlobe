package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultString;
import builderb0y.autocodec.annotations.VerifyNotEmpty;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.classes.MethodSpec.MethodSpecDesc;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstructorSpec extends MemberSpec {

	public final @IdentifierName @DefaultString("new") String name;
	public final @VerifyNotEmpty List<String> values;

	public ConstructorSpec(String name, List<String> values) {
		this.name = name;
		this.values = values;
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		ConstructorContext context = new ConstructorContext();
		context.resolvedParameters = this.values.stream().map((String name) -> {
			for (RegistryEntry<ElementSpec> entry = hierarchy.entryOf(owner); entry != null && entry.value() instanceof ClassSpec clazz; entry = clazz.parent) {
				for (ElementSpec element : clazz.members.objectList()) {
					if (element instanceof FieldSpec field && field.name().equals(name)) {
						return new ValueSpec(name, entry, field.type);
					}
				}
			}
			throw AutoCodecUtil.rethrow(new CustomClassFormatException("Could not find field named " + name + " in class " + UnregisteredObjectException.getID(hierarchy.entryOf(owner))));
		})
		.toArray(ValueSpec[]::new);
		context.descriptor = new MethodSpecDesc(this.name, Arrays.stream(context.resolvedParameters).map(ValueSpec::typeInfo).toList());
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
				load(new LazyVarInfo("column", ScriptedColumn.INFO.type))
			)
		)
		.emitBytecode(context.methodCompileContext);
		for (ValueSpec parameter : context.resolvedParameters) {
			putField(
				load(result),
				new FieldInfo(
					ACC_PUBLIC,
					parameter.ownerTypeInfo(),
					parameter.name,
					parameter.typeInfo()
				),
				load(new LazyVarInfo(parameter.name, parameter.typeInfo()))
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
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, ClassCompileContext caller) {
		ConstructorContext context = owner.getCompileContext(this);
		environment.addQualifiedFunctionInvokeStatic(context.methodCompileContext.info);
	}

	@Override
	public String name() {
		return this.name;
	}

	public static class ConstructorContext {

		public MethodCompileContext methodCompileContext;
		public ValueSpec[] resolvedParameters;
		public MethodSpecDesc descriptor;
	}

	public static class ValueSpec {

		public final @IdentifierName String name;
		public final RegistryEntry<ElementSpec> owner, type;

		public ValueSpec(String name, RegistryEntry<ElementSpec> owner, RegistryEntry<ElementSpec> type) {
			this.name = name;
			this.owner = owner;
			this.type = type;
		}

		public TypeInfo typeInfo() {
			return asType(this.type).getTypeInfo();
		}

		public TypeInfo ownerTypeInfo() {
			return asType(this.owner).getTypeInfo();
		}
	}
}