package builderb0y.bigglobe.columns.scripted.classes;

import java.lang.reflect.Field;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultDouble;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@UseVerifier(name = "verify", in = VoronoiClassSpec.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class VoronoiClassSpec extends BaseClassSpec {

	public final @VerifyFloatRange(min = 0.0D) @DefaultDouble(0.0D) double weight;

	public VoronoiClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable RegistryEntry<ElementSpec> parent,
		double weight,
		DelayedEntryList<ElementSpec> members
	) {
		super(name, isAbstract, parent, members);
		this.weight = weight;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, VoronoiClassSpec> context) throws VerifyException {
		VoronoiClassSpec spec = context.object;
		if (spec == null) return;

		if (spec.isAbstract && spec.weight > 0.0D) {
			throw new VerifyException(() -> "Abstract voronoi classes cannot have a weight.");
		}
	}

	@Override
	public void createClass(ClassHierarchy hierarchy) {
		super.createClass(hierarchy);
		this.primaryConstructor = this.classCompileContext.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			VoronoiBase.CONSTRUCTOR_INFO.parameterVarInfos
		);
		LoadInsnTree loadSelf = load("this", this.getTypeInfo());
		LoadInsnTree loadColumn = load("column", ScriptedColumn.INFO.type);
		invokeInstance(
			loadSelf,
			new MethodInfo(
				ACC_PUBLIC,
				this.parent != null
				? asType(this.parent).getTypeInfo()
				: VoronoiBase.INFO.type,
				"<init>",
				TypeInfos.VOID,
				VoronoiBase.CONSTRUCTOR_INFO.parameterTypeInfos
			),
			VoronoiBase.CONSTRUCTOR_INFO.loaders
		)
		.emitBytecode(this.primaryConstructor);
		this.applyDefaultFields(hierarchy, loadSelf, loadColumn);
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn) {
		throw new UnsupportedOperationException("Can't create constant voronoi instances.");
	}

	@Override
	public FieldInfo baseColumnField() {
		return VoronoiBase.INFO.column;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedField("column");
		this.overrideTracker.addReservedField("center_column");
		for (Field field : VoronoiBase.Info.class.getDeclaredFields()) {
			if (field.getType() == MethodInfo.class) try {
				MethodInfo info = (MethodInfo)(field.get(VoronoiBase.INFO));
				this.overrideTracker.addReservedMethod(info.name, info.paramTypes);
			}
			catch (IllegalAccessException exception) {
				throw new RuntimeException(exception);
			}
		}
	}
}