package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.compile.OverrideTracker;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.GetterSetterInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NormalPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final Holder<ElementSpec> property_type;
	@Override
	public TypeSpec getPropertyTypeSpec(ClassHierarchy hierarchy) {
		return requireType(this.property_type, TypeSpec.class, () -> hierarchy.idOf(this) + " > property_type");
	}
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;
	public final transient SetBasedMutableDependencyView dependencies = SetBasedMutableDependencyView.from(new HashSet<>());

	public NormalPropertySpec(
		Holder<ElementSpec> owner,
		@IdentifierName String name,
		Holder<ElementSpec> property_type,
		ScriptUsage get,
		@VerifyNullable ScriptUsage set
	) {
		super(owner);
		this.name = name;
		this.property_type = property_type;
		this.get = get;
		this.set = set;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), this.dependencies.streamDirectDependencies());
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		InsnTree loadCustomClass = params.loadCustomClass;
		TypeInfo owner = this.context.get.clazz.info;
		MethodInfo getter = this.context.get.info;
		if (this.isSettable()) {
			MethodInfo setter = this.context.set.info;
			environment.addField(
				owner,
				this.name,
				new FieldHandler.Named(
					"getter: " + getter + ", setter: " + setter,
					(ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
						if (getter.isDeprecated() || setter.isDeprecated()) {
							BigGlobeMod.LOGGER.warn("Deprecated field used: " + this.name + '\n' + parser.input.getSourceForError());
						}
						if (params.dependencies != null) params.dependencies.addDependency(self);
						return mode.makeGetterSetter(parser, receiver, getter, setter);
					}
				)
			);
			if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(owner)) {
				InsnTree tree = new GetterSetterInsnTree(loadCustomClass, getter, setter);
				environment.addVariable(this.name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name) -> {
					if (params.dependencies != null) params.dependencies.addDependency(self);
					return tree;
				}));
			}
		}
		else {
			environment.addField(getter.owner, getter.name, Handlers.builder(getter).addReceiverArgument(getter.owner).callback(params.dependencyCallback(self)).buildField());
			if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(owner)) {
				environment.addVariable(getter.name, Handlers.builder(getter).addImplicitArgument(loadCustomClass).callback(params.dependencyCallback(self)).buildVariable());
			}
		}
	}

	@Override
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		compile(hierarchy, this.context.get, this.get, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, NO_EXTRAS);
		if (this.set != null) compile(hierarchy, this.context.set, this.set, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, (MutableScriptEnvironment environment) -> {
			environment.addVariableLoad("value", this.getPropertyTypeSpec(hierarchy).getTypeInfo());
		});
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.owner(hierarchy).overrideTracker.addNormalProperty(this);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Holder<ElementSpec> getPropertyType() {
		return this.property_type;
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC;
	}
}