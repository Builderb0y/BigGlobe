package builderb0y.scripting.bytecode.tree.flow;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SwitchInsnTree implements InsnTree {

	public InsnTree value;
	/** the map's {@link Int2ObjectMap#defaultReturnValue()} is used to store the default branch. */
	public Int2ObjectSortedMap<InsnTree> cases;
	public TypeInfo type;

	public SwitchInsnTree(InsnTree value, Int2ObjectSortedMap<InsnTree> cases, TypeInfo type) {
		this.value = value;
		this.cases = cases;
		this.type  = type;
	}

	public static SwitchInsnTree create(ExpressionParser parser, InsnTree value, Int2ObjectSortedMap<InsnTree> cases) {
		if (!value.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Switch value must be single-width int");
		}
		if (cases.isEmpty()) {
			throw new IllegalArgumentException("Switch must have at least one case");
		}
		TypeInfo type = computeType(cases);
		Int2ObjectSortedMap<InsnTree> runtimeCases = new Int2ObjectAVLTreeMap<>();
		for (Int2ObjectMap.Entry<InsnTree> entry : cases.int2ObjectEntrySet()) {
			runtimeCases.put(entry.getIntKey(), entry.getValue().cast(parser, type, CastMode.IMPLICIT_THROW));
		}
		if (cases.defaultReturnValue() != null) {
			runtimeCases.defaultReturnValue(cases.defaultReturnValue().cast(parser, type, CastMode.IMPLICIT_THROW));
		}
		return new SwitchInsnTree(value, runtimeCases, type);
	}

	public static TypeInfo computeType(Int2ObjectMap<InsnTree> cases) {
		InsnTree defaultCase = cases.defaultReturnValue();
		if (defaultCase == null) {
			return TypeInfos.VOID;
		}
		else {
			return TypeMerger.computeMostSpecificType(
				Stream.concat(Stream.of(defaultCase), cases.values().stream())
				.filter(tree -> !tree.jumpsUnconditionally())
				.map(InsnTree::getTypeInfo)
				.toArray(TypeInfo.ARRAY_FACTORY)
			);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Reference2ObjectMap<InsnTree, Label> starts = new Reference2ObjectOpenHashMap<>(this.cases.size());
		this.cases.values().forEach(body -> starts.computeIfAbsent(body, $ -> label()));
		InsnTree defaultCase = this.cases.defaultReturnValue();
		if (defaultCase != null) {
			starts.computeIfAbsent(defaultCase, $ -> label());
		}
		Label end = label();
		int minKey = this.cases.firstIntKey();
		int maxKey = this.cases.lastIntKey();
		int range = maxKey - minKey + 1;
		int occupancy = this.cases.size();
		//step 1: emit the TABLESWITCH or LOOKUPSWITCH instruction.
		this.value.emitBytecode(method);
		if (occupancy < range >> 2) { //sparse, use lookup switch.
			method.node.visitLookupSwitchInsn(
				starts.getOrDefault(defaultCase, end),
				this.cases.keySet().toIntArray(),
				this.cases.values().stream().map(starts::get).toArray(Label[]::new)
			);
		}
		else { //dense, use table switch.
			Label[] labels = new Label[range];
			for (Int2ObjectMap.Entry<InsnTree> entry : this.cases.int2ObjectEntrySet()) {
				labels[entry.getIntKey() - minKey] = starts.get(entry.getValue());
			}
			Label defaultLabel = starts.getOrDefault(defaultCase, end);
			for (int index = 0; index < range; index++) {
				if (labels[index] == null) {
					labels[index] = defaultLabel;
				}
			}
			method.node.visitTableSwitchInsn(minKey, maxKey, defaultLabel, labels);
		}
		//step 2: emit the cases.
		for (Map.Entry<InsnTree, Label> entry : starts.entrySet()) {
			method.node.visitLabel(entry.getValue());
			entry.getKey().emitBytecode(method);
			method.node.visitJumpInsn(GOTO, end);
		}
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public boolean jumpsUnconditionally() {
		InsnTree defaultCase = this.cases.defaultReturnValue();
		return (
			defaultCase != null &&
			defaultCase.jumpsUnconditionally() &&
			this.cases.values().stream().allMatch(InsnTree::jumpsUnconditionally)
		);
	}

	public SwitchInsnTree mapCases(UnaryOperator<InsnTree> mapper, TypeInfo type) {
		Int2ObjectSortedMap<InsnTree> newCases = new Int2ObjectAVLTreeMap<>();
		for (Int2ObjectMap.Entry<InsnTree> entry : this.cases.int2ObjectEntrySet()) {
			InsnTree mapped = mapper.apply(entry.getValue());
			if (mapped == null) return null;
			newCases.put(entry.getIntKey(), mapped);
		}
		if (this.cases.defaultReturnValue() != null) {
			InsnTree mapped = mapper.apply(this.cases.defaultReturnValue());
			if (mapped == null) return null;
			newCases.defaultReturnValue(mapped);
		}
		return new SwitchInsnTree(this.value, newCases, type);
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		return this.mapCases(branch -> branch.cast(parser, type, mode), type);
	}

	@Override
	public boolean canBeStatement() {
		InsnTree defaultCase = this.cases.defaultReturnValue();
		return (
			(defaultCase == null || defaultCase.canBeStatement()) &&
			this.cases.values().stream().allMatch(InsnTree::canBeStatement)
		);
	}

	@Override
	public InsnTree asStatement() {
		return this.mapCases(InsnTree::asStatement, TypeInfos.VOID);
	}
}