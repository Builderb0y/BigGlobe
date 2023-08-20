package builderb0y.scripting.bytecode.tree.instructions.between;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

public abstract class BetweenInsnTree implements InsnTree {

	public InsnTree value, min, max;
	public boolean minInclusive, maxInclusive;

	public BetweenInsnTree(InsnTree value, InsnTree min, boolean minInclusive, InsnTree max, boolean maxInclusive) {
		this.value = value;
		this.min = min;
		this.max = max;
		this.minInclusive = minInclusive;
		this.maxInclusive = maxInclusive;
	}

	public static InsnTree create(
		ExpressionParser parser,
		InsnTree value,
		InsnTree min,
		boolean minInclusive,
		InsnTree max,
		boolean maxInclusive
	) {
		return switch (TypeInfos.widenToInt(value.getTypeInfo()).getSort()) {
			case INT -> IntBetweenInsnTree.create(parser, value, min, minInclusive, max, maxInclusive);
			case LONG -> LongBetweenInsnTree.create(parser, value, min, minInclusive, max, maxInclusive);
			case FLOAT -> FloatBetweenInsnTree.create(parser, value, min, minInclusive, max, maxInclusive);
			case DOUBLE -> DoubleBetweenInsnTree.create(parser, value, min, minInclusive, max, maxInclusive);
			default -> throw new IllegalArgumentException("Can only call isBetween" + (minInclusive ? '[' : '(') + (maxInclusive ? ']' : ')') + " on numbers.");
		};
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.BOOLEAN;
	}
}