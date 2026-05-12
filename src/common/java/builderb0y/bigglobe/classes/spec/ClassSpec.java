package builderb0y.bigglobe.classes.spec;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.MapData;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.ScriptObject;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;
import net.minecraft.core.Holder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ClassSpec extends BaseClassSpec {

	public ClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable Holder<ElementSpec> parent,
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
			TypeInfos.VOID
		);
		invokeInstance(
			load("this", this.getTypeInfo()),
			new MethodInfo(
				ACC_PUBLIC,
				this.parent != null
					? asType(this.parent).getTypeInfo()
					: this.defaultSuperClass(),
				"<init>",
				TypeInfos.VOID
			)
		)
			.emitBytecode(this.primaryConstructor);
	}

	@Override
	public void compileMembers(ClassHierarchy hierarchy) throws ScriptParsingException {
		this.applyDefaultFields(hierarchy, load("this", this.getTypeInfo()));
		this.primaryConstructor.node.visitInsn(RETURN);
		this.primaryConstructor.endCode();
		super.compileMembers(hierarchy);
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data, InsnTree loadColumn) throws ConstantFormatException {
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
	public TypeInfo defaultSuperClass() {
		return ScriptObject.TYPE;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedField("column");
	}
}