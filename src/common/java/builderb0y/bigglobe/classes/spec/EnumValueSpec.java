package builderb0y.bigglobe.classes.spec;

import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EnumValueSpec extends MemberSpec {

	@Override
	public EnumClassSpec owner(ClassHierarchy hierarchy) {
		return requireType(this.owner, EnumClassSpec.class, () -> hierarchy.idOf(this) + " > owner");
	}
	public final Holder<ElementSpec> impl_type;
	public EnumClassSpec implType(ClassHierarchy hierarchy) {
		return requireType(this.impl_type, EnumClassSpec.class, () -> hierarchy.idOf(this) + " > impl_type");
	}
	public final @IdentifierName String name;
	public final @VerifyNullable Map<String, Data> field_values;

	public transient FieldCompileContext context;

	public EnumValueSpec(
		Holder<ElementSpec> owner,
		Holder<ElementSpec> impl_type,
		@IdentifierName String name,
		@VerifyNullable Map<String, Data> field_values
	) {
		super(owner);
		this.impl_type = impl_type;
		this.name = name;
		this.field_values = field_values;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), Stream.of(this.impl_type));
	}

	@Override
	@MustBeInvokedByOverriders
	public void reference(ClassHierarchy hierarchy) throws DetailedException {
		super.reference(hierarchy);
		Holder<ElementSpec> old = this.owner(hierarchy).values.putIfAbsent(this.name, hierarchy.entryOf(this));
		if (old != null) {
			throw new CustomClassFormatException("Duplicate enum values in class " + UnregisteredObjectException.getID(this.owner) + " with name " + this.name + " provided by " + UnregisteredObjectException.getID(old) + " and " + hierarchy.idOf(this));
		}
	}

	@Override
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		EnumClassSpec base = this.owner(hierarchy);
		EnumClassSpec impl = this.implType(hierarchy);
		while (impl != base) {
			if (impl.parent != null) {
				impl = impl.parent(hierarchy);
			}
			else {
				throw new CustomClassFormatException(hierarchy.idOf(this) + " uses an impl_type of " + UnregisteredObjectException.getID(this.impl_type) + ", but that type does not extend the owner type, which is " + UnregisteredObjectException.getID(this.owner));
			}
		}
	}

	@Override
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.context = (
			this.owner(hierarchy).classCompileContext.newField(
				ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
				this.name,
				this.implType(hierarchy).getTypeInfo()
			)
		);
	}

	@Override
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		super.compile(hierarchy);
		FieldCompileContext context = this.context;
		putStatic(
			context.info,
			newInstance(
				this.implType(hierarchy).primaryConstructor.info,
				Stream
				.concat(
					Stream.of(ldc(this.name)),

					this
					.implType(hierarchy)
					.getMembers(ConstantFieldSpec.class, true)
					.map((ConstantFieldSpec field) -> {
						if (this.field_values == null) {
							throw AutoCodecUtil.rethrow(new CustomClassFormatException("No value provided for field " + hierarchy.idOf(field) + " in " + hierarchy.idOf(this)));
						}
						Data data = this.field_values.get(field.name());
						if (data == null) {
							data = field.defaultValue;
							if (data == null) {
								throw AutoCodecUtil.rethrow(new CustomClassFormatException("No value provided for field " + hierarchy.idOf(field) + " in " + hierarchy.idOf(this)));
							}
						}
						try {
							return field.fieldType(hierarchy).parseConstant(hierarchy, data);
						}
						catch (ConstantFormatException exception) {
							throw AutoCodecUtil.rethrow(exception);
						}
					})
				)
				.toArray(InsnTree.ARRAY_FACTORY)
			)
		)
		.emitBytecode(this.owner(hierarchy).staticInitializer);
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		environment.addQualifiedVariableGetStatic(((BaseClassSpec)(this.owner.value())).getTypeInfo(), this.name, this.context.info);
	}

	@Override
	public String toString() {
		return "EnumValueSpec " + UnregisteredObjectException.getID(this.owner) + "." + this.name;
	}

	@Override
	public String name() {
		return this.name;
	}
}