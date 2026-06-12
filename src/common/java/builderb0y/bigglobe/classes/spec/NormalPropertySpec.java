package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
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
	public void setupEnvironment(Holder<ElementSpec> self, ExpressionParser parser, ExternalEnvironmentParams params) {
		InsnTree loadCustomClass = params.loadCustomClass;
		TypeInfo owner = this.context.get.clazz.info;
		MethodInfo getter = this.context.get.info;
		if (this.isSettable()) {
			MethodInfo setter = this.context.set.info;
			parser.environment.mutable().addField(
				new FieldHandler.Named(
					owner,
					this.name,
					"getter: " + getter + ", setter: " + setter,
					params.dependencyCallback(self),
					(ExpressionParser parser_, InsnTree receiver, String name, GetFieldMode mode) -> {
						if (getter.isDeprecated() || setter.isDeprecated()) {
							BigGlobeMod.LOGGER.warn("Deprecated field used: " + this.name + '\n' + parser_.input.getSourceForError());
						}
						return mode.makeGetterSetter(parser_, receiver, getter, setter);
					}
				)
			);
		}
		else {
			parser.environment.mutable().addField(Handlers.methodBuilder(getter).addReceiverArgument(getter.owner).onUsed(params.dependencyCallback(self)).buildField());
		}
	}

	@Override
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		compile(hierarchy, this.context.get, this.get, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, NO_EXTRAS);
		if (this.set != null) compile(hierarchy, this.context.set, this.set, load("this", this.owner(hierarchy).getTypeInfo()), this.dependencies, (ExpressionParser parser) -> {
			parser.environment.mutable().addVariableLoad("value", this.getPropertyTypeSpec(hierarchy).getTypeInfo());
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