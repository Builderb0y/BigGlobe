package builderb0y.scripting.bytecode.tree.flow;

import java.util.Map;
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

public class SwitchInsnTree implements InsnTree {

	public InsnTree value;
	/**
	runtime bodies are guaranteed to have the same {@link InsnTree#getTypeInfo()}.
	compile bodies are not. the runtime bodies will be used for emitting bytecode,
	and the compile bodies will be used for casting {@link #cast(ExpressionParser, TypeInfo, CastMode)}.

	the map's {@link Int2ObjectMap#defaultReturnValue()} is used to store the default branch.
	*/
	public Int2ObjectSortedMap<InsnTree> compileCases, runtimeCases;
	public TypeInfo type;

	public SwitchInsnTree(InsnTree value, Int2ObjectSortedMap<InsnTree> compileCases, Int2ObjectSortedMap<InsnTree> runtimeCases, TypeInfo type) {
		this.value        = value;
		this.compileCases = compileCases;
		this.runtimeCases = runtimeCases;
		this.type         = type;
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
		return new SwitchInsnTree(value, cases, runtimeCases, type);
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
		Reference2ObjectMap<InsnTree, Label> starts = new Reference2ObjectOpenHashMap<>(this.runtimeCases.size());
		this.runtimeCases.values().forEach(body -> starts.computeIfAbsent(body, $ -> new Label()));
		InsnTree defaultCase = this.runtimeCases.defaultReturnValue();
		if (defaultCase != null) {
			starts.computeIfAbsent(defaultCase, $ -> new Label());
		}
		Label end = new Label();
		int minKey = this.runtimeCases.firstIntKey();
		int maxKey = this.runtimeCases.lastIntKey();
		int range = maxKey - minKey + 1;
		int occupancy = this.runtimeCases.size();
		//step 1: emit the TABLESWITCH or LOOKUPSWITCH instruction.
		this.value.emitBytecode(method);
		if (occupancy < range >> 2) { //sparse, use lookup switch.
			method.node.visitLookupSwitchInsn(
				starts.getOrDefault(defaultCase, end),
				this.runtimeCases.keySet().toIntArray(),
				this.runtimeCases.values().stream().map(starts::get).toArray(Label[]::new)
			);
		}
		else { //dense, use table switch.
			Label[] labels = new Label[range];
			for (Int2ObjectMap.Entry<InsnTree> entry : this.runtimeCases.int2ObjectEntrySet()) {
				labels[entry.getIntKey() - minKey] = starts.get(entry.getValue());
			}
			method.node.visitTableSwitchInsn(minKey, maxKey, starts.getOrDefault(defaultCase, end), labels);
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
		InsnTree defaultCase = this.compileCases.defaultReturnValue();
		return (
			defaultCase != null &&
			defaultCase.jumpsUnconditionally() &&
			this.compileCases.values().stream().allMatch(InsnTree::jumpsUnconditionally)
		);
	}

	@Override
	public boolean canBeStatement() {
		InsnTree defaultCase = this.compileCases.defaultReturnValue();
		return (
			(defaultCase == null || defaultCase.canBeStatement()) &&
			this.compileCases.values().stream().allMatch(InsnTree::canBeStatement)
		);
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		Int2ObjectSortedMap<InsnTree> newCases = new Int2ObjectAVLTreeMap<>();
		for (Int2ObjectMap.Entry<InsnTree> entry : this.compileCases.int2ObjectEntrySet()) {
			InsnTree cast = entry.getValue().cast(parser, type, mode);
			if (cast == null) return null;
			newCases.put(entry.getIntKey(), cast);
		}
		if (this.compileCases.defaultReturnValue() != null) {
			InsnTree cast = this.compileCases.defaultReturnValue().cast(parser, type, mode);
			if (cast == null) return null;
			newCases.defaultReturnValue(cast);
		}
		return new SwitchInsnTree(this.value, newCases, newCases, type);
	}
}