package builderb0y.scripting.bytecode.loops;

import java.util.List;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForDoubleRangeInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForFloatRangeInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIntRangeInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForLongRangeInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class RangeLoopFactory implements LoopFactory {

	public boolean ascending;
	public InsnTree lowerBound, upperBound;
	public boolean lowerBoundInclusive, upperBoundInclusive;
	public InsnTree step;

	public RangeLoopFactory(
		boolean ascending,
		InsnTree lowerBound,
		boolean lowerBoundInclusive,
		InsnTree upperBound,
		boolean upperBoundInclusive,
		InsnTree step
	) {
		this.ascending = ascending;
		this.lowerBound = lowerBound;
		this.lowerBoundInclusive = lowerBoundInclusive;
		this.upperBound = upperBound;
		this.upperBoundInclusive = upperBoundInclusive;
		this.step = step;
	}

	@Override
	public InsnTree createLoop(ExpressionParser parser, LoopName loopName, List<VariableDeclarationInsnTree> variables, InsnTree body) throws ScriptParsingException {
		for (int index = variables.size(); --index >= 0;) {
			if (variables.get(index).variable.type.getSort() != this.lowerBound.getTypeInfo().getSort()) {
				throw new ScriptParsingException("variable type (" + variables.get(index).getTypeInfo() + ") does not match range type (" + this.lowerBound.getTypeInfo() + ')', parser.input);
			}
			LazyVarInfo lowerBoundVariable = this.lowerBound.getConstantValue().isConstant() ? null : new LazyVarInfo(parser.method.mangleName("lowerBound"), this.lowerBound.getTypeInfo());
			LazyVarInfo upperBoundVariable = this.upperBound.getConstantValue().isConstant() ? null : new LazyVarInfo(parser.method.mangleName("upperBound"), this.upperBound.getTypeInfo());
			LazyVarInfo stepVariable = this.step.getConstantValue().isConstant() ? null : new LazyVarInfo(parser.method.mangleName("step"), this.step.getTypeInfo());
			body = switch (this.lowerBound.getTypeInfo().getSort()) {
				case INT -> new ForIntRangeInsnTree(
					loopName,
					variables.get(index),
					this.ascending,
					this.lowerBound,
					this.lowerBoundInclusive,
					lowerBoundVariable,
					this.upperBound,
					this.upperBoundInclusive,
					upperBoundVariable,
					this.step,
					stepVariable,
					body
				);
				case LONG -> new ForLongRangeInsnTree(
					loopName,
					variables.get(index),
					this.ascending,
					this.lowerBound,
					this.lowerBoundInclusive,
					lowerBoundVariable,
					this.upperBound,
					this.upperBoundInclusive,
					upperBoundVariable,
					this.step,
					stepVariable,
					body
				);
				case FLOAT -> new ForFloatRangeInsnTree(
					loopName,
					variables.get(index),
					this.ascending,
					this.lowerBound,
					this.lowerBoundInclusive,
					lowerBoundVariable,
					this.upperBound,
					this.upperBoundInclusive,
					upperBoundVariable,
					this.step,
					stepVariable,
					body
				);
				case DOUBLE -> new ForDoubleRangeInsnTree(
					loopName,
					variables.get(index),
					this.ascending,
					this.lowerBound,
					this.lowerBoundInclusive,
					lowerBoundVariable,
					this.upperBound,
					this.upperBoundInclusive,
					upperBoundVariable,
					this.step,
					stepVariable,
					body
				);
				default -> throw new ScriptParsingException("range type must be int, long, float, or double", parser.input);
			};
		}
		return body;
	}
}