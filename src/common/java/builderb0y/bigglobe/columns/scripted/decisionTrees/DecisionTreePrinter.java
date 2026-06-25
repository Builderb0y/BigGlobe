package builderb0y.bigglobe.columns.scripted.decisionTrees;

import java.math.BigInteger;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class DecisionTreePrinter {

	public String name;
	public DecisionTreePrinter ifMatch, unlessMatch;
	public int depth = -1;
	/**
	for each bit: 0 means this is the unlessMatch child of our parent,
	and 1 means this is the ifMatch child of our parent.
	the most significant bit corresponds to this printer's direct parent.
	the least significant bit is for the root node.
	*/
	public BigInteger path = BigInteger.ZERO;

	public DecisionTreePrinter(String name) {
		this.name = name;
	}

	public static void printIfEnabled(Identifier id, Holder<DecisionTreeSpec> root, Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches) {
		if (BigGlobeConfig.INSTANCE.get().dataPackDebugging.decisionTrees) {
			BigGlobeMod.LOGGER.info(
				DecisionTreePrinter.parse(root, patches).print(
					new StringBuilder(1024)
					.append(id)
					.append(" decision tree, as requested in Big Globe's config file:\n")
				)
				.toString()
			);
		}
	}

	public static DecisionTreePrinter parse(Holder<DecisionTreeSpec> root, Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches) {
		DecisionTreePrinter printer = convert(root, patches);
		printer.updateDepthSize(0, BigInteger.ZERO);
		return printer;
	}

	public static DecisionTreePrinter convert(Holder<DecisionTreeSpec> entry, Map<Holder<DecisionTreeSpec>, Holder<DecisionTreeSpec>> patches) {
		if (patches != null) entry = patches.getOrDefault(entry, entry);
		DecisionTreePrinter printer = new DecisionTreePrinter(UnregisteredObjectException.getID(entry).toString());
		switch (entry.value()) {
			case ConditionDecisionTreeSpec condition -> {
				printer.ifMatch = convert(condition.if_true, patches);
				printer.unlessMatch = convert(condition.if_false, patches);
			}
			case BorderDecisionTreeSpec border -> {
				printer.ifMatch = convert(border.if_positive, patches);
				printer.unlessMatch = convert(border.if_negative, patches);
			}
			default -> {}
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