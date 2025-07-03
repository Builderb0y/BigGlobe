package builderb0y.bigglobe.columns.scripted.classes;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassSpec extends BaseClassSpec {

	public ClassSpec(
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
			ObjectBase.CONSTRUCTOR_INFO.parameterVarInfos
		);
		LoadInsnTree loadSelf = load("this", this.getTypeInfo());
		LoadInsnTree loadColumn = load("column", ScriptedColumn.INFO.type);
		invokeInstance(
			loadSelf,
			new MethodInfo(
				ACC_PUBLIC,
				this.parent != null
				? asType(this.parent).getTypeInfo()
				: ObjectBase.INFO.type,
				"<init>",
				TypeInfos.VOID,
				ObjectBase.CONSTRUCTOR_INFO.parameterTypeInfos
			),
			ObjectBase.CONSTRUCTOR_INFO.loaders
		)
		.emitBytecode(this.primaryConstructor);
		this.applyDefaultFields(hierarchy, loadSelf, loadColumn);
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn) {
		if (this.isAbstract) {
			throw new IllegalArgumentException("Can't create constant for abstract class " + UnregisteredObjectException.getID(hierarchy.entryOf(this)));
		}
		MapData map = data.tryAsMap();
		if (map == null) {
			throw new IllegalArgumentException("Not a map: " + data);
		}
		InsnTree result = newInstance(this.primaryConstructor.info, loadColumn);
		result = this.applyFields(hierarchy, loadColumn, map, result);
		return result;
	}

	@Override
	public FieldInfo baseColumnField() {
		return ObjectBase.INFO.column;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedField("column");
	}
}