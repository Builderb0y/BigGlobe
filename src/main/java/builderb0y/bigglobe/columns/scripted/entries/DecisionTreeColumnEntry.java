package builderb0y.bigglobe.columns.scripted.entries;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DecisionTreeColumnEntry extends AbstractColumnEntry {

	public final RegistryEntry<DecisionTreeSettings> root;

	public DecisionTreeColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		@DefaultBoolean(true) boolean cache,
		RegistryEntry<DecisionTreeSettings> root
	) {
		super(params, valid, cache);
		this.root = root;
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, context, null)).emitBytecode(computeMethod);
		computeMethod.endCode();
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, context, load("y", TypeInfos.INT))).emitBytecode(computeMethod);
		computeMethod.endCode();
	}
}