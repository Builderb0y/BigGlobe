package builderb0y.bigglobe.columns.scripted.entries;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchemas.BlockState2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.Valids.BlockState2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.settings.Seed;
import builderb0y.bigglobe.settings.Seed.AutoSeed;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockStateDecisionTree2DColumnEntry extends Basic2DColumnEntry {

	public final RegistryEntry<DecisionTreeSettings> value;
	public final Seed seed;
	public final @VerifyNullable BlockState2DValid valid;
	public final @DefaultBoolean(true) boolean cache;

	public BlockStateDecisionTree2DColumnEntry(RegistryEntry<DecisionTreeSettings> value, Seed seed, BlockState2DValid valid, @DefaultBoolean(true) boolean cache) {
		this.value = value;
		this.seed  = seed;
		this.valid = valid;
		this.cache = cache;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	@Override
	public void populateCompute(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		long seed;
		if (this.seed instanceof AutoSeed) {
			seed = Permuter.permute(0L, UnregisteredObjectException.getID(this.value));
		}
		else {
			seed = this.seed.value;
		}
		return_(this.value.value().createInsnTree(this.value, seed, context, null)).emitBytecode(computeMethod);
		computeMethod.endCode();
	}

	@Override
	public AccessSchema getAccessSchema() {
		return BlockState2DAccessSchema.INSTANCE;
	}
}