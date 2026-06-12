package builderb0y.bigglobe.classes.spec;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AbstractPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final Holder<ElementSpec> property_type;
	@Override
	public TypeSpec getPropertyTypeSpec(ClassHierarchy hierarchy) {
		return requireType(this.property_type, TypeSpec.class, () -> hierarchy.idOf(this) + " > property_type");
	}
	public final boolean settable;

	public AbstractPropertySpec(
		Holder<ElementSpec> owner,
		@IdentifierName String name,
		Holder<ElementSpec> property_type,
		boolean settable
	) {
		super(owner);
		this.name = name;
		this.property_type = property_type;
		this.settable = settable;
	}

	@Override
	public boolean isSettable() {
		return this.settable;
	}

	@Override
	public Holder<ElementSpec> getPropertyType() {
		return this.property_type;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC | ACC_ABSTRACT;
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		this.owner(hierarchy).overrideTracker.addAbstractProperty(this);
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, ExpressionParser parser, ExternalEnvironmentParams params) {
		MethodInfo getter = this.context.get.info;
		if (this.settable) {
			TypeInfo owner = this.context.get.clazz.info;
			MethodInfo setter = this.context.set.info;
			parser.environment.mutable().addField(
				new FieldHandler.Named(
					owner,
					this.name,
					this.name + ": getter: " + getter + ", setter: " + setter,
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
	public String name() {
		return this.name;
	}
}