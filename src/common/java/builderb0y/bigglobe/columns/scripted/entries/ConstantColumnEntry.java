package builderb0y.bigglobe.columns.scripted.entries;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ConstantColumnEntry implements ColumnEntry, SimpleDependencyView {

	public final AccessSchema params;
	public final Data value;

	public ConstantColumnEntry(AccessSchema params, Data value) {
		this.params = params;
		this.value = value;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}

	@Override
	public boolean hasField() {
		return false;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return this.params;
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		ColumnEntry.super.populateGetter(memory, context, getterMethod);
		return_(this.params.createConstant(this.value, context.root())).emitBytecode(getterMethod);
		getterMethod.endCode();
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {

	}
}