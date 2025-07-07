package builderb0y.bigglobe.columns.scripted.classes;

import java.lang.reflect.Field;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiClassSpec extends BaseClassSpec {

	public VoronoiClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable RegistryEntry<ElementSpec> parent,
		DelayedEntryList<ElementSpec> members
	) {
		super(name, isAbstract, parent, members);
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
		invokeInstance(
			load("this", this.getTypeInfo()),
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
	}

	@Override
	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {
		this.applyDefaultFields(hierarchy, load("this", this.getTypeInfo()), load("column", ScriptedColumn.INFO.type));
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
		super.compileMembers(hierarchy);
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