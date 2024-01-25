package builderb0y.bigglobe.columns.scripted.compile;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiImplCompileContext extends DataCompileContext {

	public final VoronoiBaseCompileContext parent;

	public VoronoiImplCompileContext(VoronoiBaseCompileContext parent, long seed) {
		parent.children.add(this);
		this.parent = parent;
		this.flagsIndex = VoronoiDataBase.BUILTIN_FLAG_COUNT;
		this.mainClass = parent.mainClass.newInnerClass(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			Type.getInternalName(VoronoiDataBase.class) + "$Generated$Impl_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			parent.mainClass.info,
			new TypeInfo[0]
		);

		LazyVarInfo column, cell;
		this.constructor = this.mainClass.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			column = new LazyVarInfo("column", parent.root().columnType()),
			cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class))
		);
		LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
		invokeInstance(
			load(self),
			new MethodInfo(
				ACC_PUBLIC,
				parent.mainClass.info,
				"<init>",
				TypeInfos.VOID,
				parent.root().columnType(),
				type(VoronoiDiagram2D.Cell.class),
				TypeInfos.LONG
			),
			load(column),
			load(cell),
			ldc(seed)
		)
		.emitBytecode(this.constructor);
	}

	@Override
	public ColumnCompileContext root() {
		return this.parent.root();
	}

	@Override
	public MutableScriptEnvironment environment() {
		return this.parent.environment().addAll(this.environment);
	}

	@Override
	public InsnTree loadSelf() {
		return load("this", this.selfType());
	}

	@Override
	public InsnTree loadColumn() {
		return getField(
			this.loadSelf(),
			new FieldInfo(
				ACC_PUBLIC,
				this.voronoiBaseType(),
				"column",
				this.root().columnType()
			)
		);
	}

	@Override
	public InsnTree loadSeed() {
		return getField(
			this.loadSelf(),
			new FieldInfo(
				ACC_PUBLIC,
				type(VoronoiDataBase.class),
				"seed",
				TypeInfos.LONG
			)
		);
	}

	@Override
	public FieldInfo flagsField(int index) {
		return new FieldInfo(
			ACC_PUBLIC,
			(index >>> 5) == 0 ? type(VoronoiDataBase.class) : this.mainClass.info,
			"flags_" + (index >>> 5),
			TypeInfos.INT
		);
	}

	@Override
	public TypeInfo voronoiBaseType() {
		return this.parent.voronoiBaseType();
	}

	@Override
	public void prepareForCompile() {
		for (int index = 1, max = this.flagsIndex >>> 5; index <= max; index++) {
			this.mainClass.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
		}
		super.prepareForCompile();
	}
}