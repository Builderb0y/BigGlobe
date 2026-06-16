package builderb0y.bigglobe.columns.scripted.entries;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.ColumnCompileContext;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantColumnEntry extends ColumnEntry {

	public final Data value;

	public ConstantColumnEntry(AccessSchema params, Data value) {
		super(params);
		this.value = value;
	}

	@Override
	public boolean hasFieldSetterAndFlag() {
		return false;
	}

	@Override
	public void createRepresentation(ColumnEntryRegistry registry) throws DetailedException {
		super.createRepresentation(registry);
		ColumnEntryContext context = new ColumnEntryContext();
		context.uniquifier = registry.columnCompileContext.clazz.memberUniquifier++;
		context.internalName = ColumnCompileContext.internalName(UnregisteredObjectException.getID(registry.entryOf(this)), context.uniquifier);
		context.mainGetter = registry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			"get_" + context.internalName,
			this.getTypeInfo(registry),
			this.params.is_3d()
			? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) }
			: LazyVarInfo.ARRAY_FACTORY.empty()
		);
		registry.columnCompileContext.setCompileContext(this, context);
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws DetailedException {
		super.compile(registry);
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		return_(this.params.typeSpec(registry, this).parseConstant(registry.classHierarchy, this.value)).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}
}