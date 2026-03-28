package builderb0y.bigglobe.columns.scripted.entries;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.decisionTrees.ConditionBasedDecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DecisionTreeColumnEntry extends AbstractColumnEntry {

	public final Holder<DecisionTreeSettings> root;
	public final @VerifyNullable Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches;

	public DecisionTreeColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		@DefaultBoolean(true) boolean cache,
		Holder<DecisionTreeSettings> root,
		@VerifyNullable Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches,
		DecodeContext<?> decodeContext
	) {
		super(params, valid, cache, decodeContext);
		this.root = root;
		this.patches = patches;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		Stream<Holder<? extends DependencyView>> result = Stream.concat(super.streamDirectDependencies(), Stream.of(this.root));
		if (this.patches != null) {
			result = Stream.of(result, this.patches.keySet().stream(), this.patches.values().stream()).flatMap(Function.identity());
		}
		return result;
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, this.params, context, this.patches, null)).emitBytecode(computeMethod);
		computeMethod.endCode();
		this.printIfEnabled(memory);
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, this.params, context, this.patches, load("y", TypeInfos.INT))).emitBytecode(computeMethod);
		computeMethod.endCode();
		this.printIfEnabled(memory);
	}

	public void printIfEnabled(ColumnEntryMemory memory) {
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.decisionTrees) {
			BigGlobeMod.LOGGER.info(
				Printer.parse(this.root, this.patches).print(
						new StringBuilder(1024)
							.append(memory.getTyped(ColumnEntryMemory.ACCESSOR_ID))
							.append(" decision tree, as requested in Big Globe's config file:\n")
					)
					.toString()
			);
		}
	}

	public static class Printer {

		public String name;
		public Printer ifMatch, unlessMatch;
		public int depth = -1;
		/**
		for each bit: 0 means this is the unlessMatch child of our parent,
		and 1 means this is the ifMatch child of our parent.
		the most significant bit corresponds to this printer's direct parent.
		the least significant bit is for the root node.
		*/
		public BigInteger path = BigInteger.ZERO;

		public Printer(String name) {
			this.name = name;
		}

		public static Printer parse(Holder<DecisionTreeSettings> root, Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches) {
			Printer printer = convert(root, patches);
			printer.updateDepthSize(0, BigInteger.ZERO);
			return printer;
		}

		public static Printer convert(Holder<DecisionTreeSettings> entry, Map<Holder<DecisionTreeSettings>, Holder<DecisionTreeSettings>> patches) {
			if (patches != null) entry = patches.getOrDefault(entry, entry);
			Printer printer = new Printer(UnregisteredObjectException.getID(entry).toString());
			if (entry.value() instanceof ConditionBasedDecisionTreeSettings condition) {
				printer.ifMatch = convert(condition.if_true, patches);
				printer.unlessMatch = convert(condition.if_false, patches);
			}
			return printer;
		}

		public void updateDepthSize(int depth, BigInteger path) {
			this.depth = depth;
			this.path = path;
			if (this.ifMatch != null) {
				this.ifMatch.updateDepthSize(depth + 1, path.setBit(depth));
			}
			if (this.unlessMatch != null) {
				this.unlessMatch.updateDepthSize(depth + 1, path);
			}
		}

		public StringBuilder print(StringBuilder builder) {
			if (this.ifMatch != null) this.ifMatch.print(builder);
			BigInteger bits = this.path.xor(this.path.shiftRight(1));
			for (int index = 0; index < this.depth; index++) {
				builder.append(
					index == this.depth - 1
						? (this.path.testBit(this.depth - 1) ? "┌───" : "└───")
						: (bits.testBit(index) ? "│   " : "    ")
				);
			}
			builder.append(this.name).append('\n');
			if (this.unlessMatch != null) this.unlessMatch.print(builder);
			return builder;
		}
	}
}