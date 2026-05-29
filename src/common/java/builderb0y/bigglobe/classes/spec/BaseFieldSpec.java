package builderb0y.bigglobe.classes.spec;

import java.util.stream.Stream;

import it.unimi.dsi.fastutil.Hash.Strategy;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.InsnTrees;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FieldHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.VariableHandler;
import builderb0y.scripting.environments.ScriptEnvironment.GetFieldMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public abstract class BaseFieldSpec extends MemberSpec {

	public static final Strategy<BaseFieldSpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), (BaseFieldSpec field) -> field.field_type),
		FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

	public final @IdentifierName String name;
	public final Holder<ElementSpec> field_type;
	public TypeSpec fieldType(ClassHierarchy hierarchy) {
		return requireType(this.field_type, TypeSpec.class, () -> hierarchy.idOf(this) + " > field_type");
	}
	public transient Context context;

	public BaseFieldSpec(Holder<ElementSpec> owner, @IdentifierName String name, Holder<ElementSpec> field_type) {
		super(owner);
		this.name = name;
		this.field_type = field_type;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), Stream.of(this.field_type));
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		if (this.fieldType(hierarchy).getTypeInfo().isVoid()) {
			throw new CustomClassFormatException("Void-typed field: " + hierarchy.idOf(this));
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		BaseClassSpec owner = this.owner(hierarchy);
		this.context = new Context();
		this.context.field = owner.classCompileContext.newField(
			this.getAccessFlags(),
			this.name,
			this.fieldType(hierarchy).getTypeInfo()
		);
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		FieldInfo fieldInfo = this.context.field.info;
		environment.addField(fieldInfo.owner, fieldInfo.name, new FieldHandler.Named(fieldInfo.toString(), (ExpressionParser parser, InsnTree receiver, String name, GetFieldMode mode) -> {
			if (params.dependencies != null) params.dependencies.addDependency(self);
			return mode.makeField(parser, receiver, fieldInfo);
		}
		));
		InsnTree loadCustomClass = params.loadCustomClass;
		if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(this.context.field.clazz.info)) {
			InsnTree tree = InsnTrees.getField(loadCustomClass, fieldInfo);
			environment.addVariable(fieldInfo.name, new VariableHandler.Named(tree.describe(), (ExpressionParser parser, String name) -> {
				if (params.dependencies != null) params.dependencies.addDependency(self);
				return tree;
			}));
		}
	}

	public abstract @Nullable InsnTree getDefaultValue(ClassHierarchy hierarchy) throws DetailedException;

	public abstract int getAccessFlags();

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " " + this.field_type.value().name() + " " + this.owner.value().name() + "." + this.name;
	}

	public static class Context {

		public FieldCompileContext field;
		//non-null if, and only if, the field allows script-based initializers.
		public @Nullable MethodCompileContext defaultValueMethod;
	}
}