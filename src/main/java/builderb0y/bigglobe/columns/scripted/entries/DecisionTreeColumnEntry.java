package builderb0y.bigglobe.columns.scripted.entries;

import java.math.BigInteger;
import java.util.Set;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.decisionTrees.DecisionTreeSettings;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.UnregisteredObjectException;
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
		RegistryEntry<DecisionTreeSettings> root,
		DecodeContext<?> decodeContext
	) {
		super(params, valid, cache, decodeContext);
		this.root = root;
	}

	@Override
	public Set<RegistryEntry<? extends ColumnValueDependencyHolder>> getDependencies() {
		return this.root.value().getDependencies();
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, this.params, context, null)).emitBytecode(computeMethod);
		computeMethod.endCode();
		this.printIfEnabled(memory);
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		return_(this.root.value().createInsnTree(this.root, this.params, context, load("y", TypeInfos.INT))).emitBytecode(computeMethod);
		computeMethod.endCode();
		this.printIfEnabled(memory);
	}

	public void printIfEnabled(ColumnEntryMemory memory) {
		if (BigGlobeConfig.INSTANCE.get().printDecisionTrees) {
			BigGlobeMod.LOGGER.info(
				Printer.parse(this.root).print(
					new StringBuilder(128)
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

		public static Printer parse(RegistryEntry<DecisionTreeSettings> root) {
			Printer printer = convert(root);
			printer.updateDepthSize(0, BigInteger.ZERO);
			return printer;
		}

		public static Printer convert(RegistryEntry<DecisionTreeSettings> entry) {
			if (entry == null) return null;
			Printer printer = new Printer(UnregisteredObjectException.getID(entry).toString());
			printer.ifMatch = convert(entry.value().if_true);
			printer.unlessMatch = convert(entry.value().if_false);
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