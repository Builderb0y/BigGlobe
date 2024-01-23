package builderb0y.bigglobe.columns.scripted.entries;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.BlockState3DAccessSchema;
import builderb0y.bigglobe.columns.scripted.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.Valids.BlockState3DValid;
import builderb0y.bigglobe.columns.scripted.Valids._3DValid;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.settings.Seed.AutoSeed;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateDecisionTree3DColumnEntry extends Basic3DColumnEntry {

	public final RegistryEntry<DecisionTreeSettings> value;
	public final Seed seed;
	public final @VerifyNullable BlockState3DValid valid;

	public BlockStateDecisionTree3DColumnEntry(RegistryEntry<DecisionTreeSettings> value, Seed seed, BlockState3DValid valid) {
		this.value = value;
		this.seed  = seed;
		this.valid = valid;
	}

	@Override
	public _3DValid valid() {
		return this.valid;
	}

	@Override
	public void populateComputeAll(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeAllMethod) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void populateComputeOne(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeOneMethod) throws ScriptParsingException {
		long seed;
		if (this.seed instanceof AutoSeed) {
			seed = Permuter.permute(0L, UnregisteredObjectException.getID(this.value));
		}
		else {
			seed = this.seed.value;
		}
		return_(this.value.value().createInsnTree(this.value, seed, context, load("y", TypeInfos.INT))).emitBytecode(computeOneMethod);
	}

	@Override
	public AccessSchema getAccessSchema() {
		return BlockState3DAccessSchema.INSTANCE;
	}

	@Override
	public boolean hasField() {
		return false;
	}
}