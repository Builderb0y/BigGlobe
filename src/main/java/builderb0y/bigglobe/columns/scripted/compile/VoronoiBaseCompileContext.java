package builderb0y.bigglobe.columns.scripted.compile;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiBaseCompileContext extends DataCompileContext {

	public final ColumnCompileContext parent;

	public VoronoiBaseCompileContext(ColumnCompileContext parent) {
		parent.children.add(this);
		this.flagsIndex = 3;
		this.parent = parent;
		this.mainClass = parent.mainClass.newInnerClass(
			ACC_PUBLIC | ACC_ABSTRACT | ACC_SYNTHETIC,
			Type.getInternalName(VoronoiDataBase.class) + "$Generated$Base_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			type(VoronoiDataBase.class),
			new TypeInfo[0]
		);
		FieldCompileContext columnField = this.mainClass.newField(ACC_PUBLIC | ACC_FINAL, "column", parent.columnType());

		{
			LazyVarInfo column, cell, baseSeed;
			this.constructor = this.mainClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				column = new LazyVarInfo("column", parent.columnType()),
				cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class)),
				baseSeed = new LazyVarInfo("baseSeed", TypeInfos.LONG)
			);
			LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
			invokeInstance(
				load(self),
				new MethodInfo(
					ACC_PUBLIC,
					type(VoronoiDataBase.class),
					"<init>",
					TypeInfos.VOID,
					type(VoronoiDiagram2D.Cell.class),
					TypeInfos.LONG
				),
				load(cell),
				load(baseSeed)
			)
			.emitBytecode(this.constructor);
			putField(load(self), columnField.info, load(column)).emitBytecode(this.constructor);
		}

		{
			MethodCompileContext column = this.mainClass.newMethod(ACC_PUBLIC, "column", type(ScriptedColumn.class) /* do not use synthetic subclass, because we want to override. */);
			LazyVarInfo self = new LazyVarInfo("this", column.clazz.info);
			return_(
				getField(
					load(self),
					columnField.info
				)
			)
			.emitBytecode(column);
			column.endCode();
		}
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
				type(VoronoiDataBase.class),
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
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeInfo voronoiBaseType() {
		return this.selfType();
	}
}