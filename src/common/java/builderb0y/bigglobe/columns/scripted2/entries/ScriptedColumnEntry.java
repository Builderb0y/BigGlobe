package builderb0y.bigglobe.columns.scripted2.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public class ScriptedColumnEntry extends LoopColumnEntry {

	public final ScriptUsage script;

	public ScriptedColumnEntry(AccessSchema params, @VerifyNullable Valid valid, boolean cache, ScriptUsage script) {
		super(params, valid, cache);
		this.script = script;
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException {
		return this.makeCaller(registry, "compute_" + context.internalName, this.script, ElementSpec.asType(this.params.type()).getTypeInfo());
	}
}