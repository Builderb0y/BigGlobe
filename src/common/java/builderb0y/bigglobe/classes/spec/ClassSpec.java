package builderb0y.bigglobe.classes.spec;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.ScriptObject;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue.DynamicConstantValue;
import builderb0y.scripting.bytecode.tree.ConstantValue.HandleConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassSpec extends BaseClassSpec {

	@Override
	public ClassSpec parent(ClassHierarchy hierarchy) {
		return requireType(this.parent, ClassSpec.class, () -> hierarchy.idOf(this) + " > extends");
	}

	public ClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent
	) {
		super(name, isAbstract, parent);
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		if (this.parent != null) this.parent(hierarchy);
		super.verify(hierarchy);
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.primaryConstructor = this.classCompileContext.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID
		);
	}

	@Override
	@MustBeInvokedByOverriders
	public void compile(ClassHierarchy hierarchy) throws DetailedException {
		invokeInstance(
			load("this", this.getTypeInfo()),
			new MethodInfo(
				ACC_PUBLIC,
				this.getParentTypeInfo(hierarchy),
				"<init>",
				TypeInfos.VOID
			)
		)
		.emitBytecode(this.primaryConstructor);
		super.compile(hierarchy);
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
	}

	public static final MethodInfo CONSTANT_INVOKER = MethodInfo.findMethod(
		ConstantBootstraps.class,
		"invoke",
		Object.class,
		MethodHandles.Lookup.class,
		String.class,
		Class.class,
		MethodHandle.class,
		Object[].class
	);

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data) throws ConstantFormatException {
		if (data.isEmpty()) {
			return ldc(null, this.getTypeInfo());
		}
		else if (this.getMembers(BaseFieldSpec.class, true).findAny().isEmpty()) {
			return ldc(
				new DynamicConstantValue(
					this.getTypeInfo(),
					CONSTANT_INVOKER,
					new HandleConstantValue(this.getTypeInfo(), this.primaryConstructor.info)
				)
			);
		}
		else {
			throw new ConstantFormatException("Can't create constant for class " + hierarchy.idOf(this) + " because this class has at least one field.");
		}
	}

	@Override
	public TypeInfo defaultSuperClass() {
		return ScriptObject.TYPE;
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		super.setupEnvironment(self, environment, params);
		environment.addQualifiedConstructor(this.primaryConstructor.info);
	}
}