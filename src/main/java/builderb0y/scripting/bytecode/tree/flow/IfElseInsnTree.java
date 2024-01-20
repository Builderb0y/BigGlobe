package builderb0y.scripting.bytecode.tree.flow;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;
import builderb0y.scripting.util.TypeMerger;

public class IfElseInsnTree implements InsnTree {

	public final ConditionTree condition;
	public final InsnTree trueBody, falseBody;
	public final TypeInfo type;

	public IfElseInsnTree(
		ConditionTree condition,
		InsnTree trueBody,
		InsnTree falseBody,
		TypeInfo type
	) {
		this.condition = condition;
		this.trueBody  = trueBody;
		this.falseBody = falseBody;
		this.type      = type;
	}

	public static InsnTree create(ExpressionParser parser, ConditionTree condition, InsnTree trueBody, InsnTree falseBody) throws ScriptParsingException {
		Operands operands = Operands.of(parser, trueBody, falseBody);
		return new IfElseInsnTree(condition, operands.trueBody, operands.falseBody, operands.type);
	}

	public static record Operands(
		InsnTree trueBody,
		InsnTree falseBody,
		TypeInfo type
	) {

		public static Operands of(ExpressionParser parser, InsnTree trueBody, InsnTree falseBody) {
			TypeInfo type;
			if (trueBody.jumpsUnconditionally()) {
				if (falseBody.jumpsUnconditionally()) {
					type = TypeInfos.VOID;
				}
				else {
					type = falseBody.getTypeInfo();
				}
			}
			else {
				if (falseBody.jumpsUnconditionally()) {
					type = trueBody.getTypeInfo();
				}
				else {
					type = TypeMerger.computeMostSpecificType(trueBody.getTypeInfo(), falseBody.getTypeInfo());
					trueBody = trueBody.cast(parser, type, CastMode.IMPLICIT_THROW);
					falseBody = falseBody.cast(parser, type, CastMode.IMPLICIT_THROW);
				}
			}
			return new Operands(trueBody, falseBody, type);
		}
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Scope scope = method.scopes.pushScope();
		this.condition.emitBytecode(method, null, scope.end.getLabel());
		this.trueBody.emitBytecode(method);
		scope.cycle();
		method.scopes.cycleScope();
		method.node.visitJumpInsn(GOTO, scope.end.getLabel());
		method.node.instructions.add(scope.start);
		this.falseBody.emitBytecode(method);
		method.scopes.popLoop();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public boolean jumpsUnconditionally() {
		if (this.condition instanceof ConstantConditionTree constant) {
			return (constant.value ? this.trueBody : this.falseBody).jumpsUnconditionally();
		}
		else {
			return this.trueBody.jumpsUnconditionally() && this.falseBody.jumpsUnconditionally();
		}
	}

	@Override
	public boolean canBeStatement() {
		return this.trueBody.canBeStatement() && this.falseBody.canBeStatement();
	}

	@Override
	public InsnTree asStatement() {
		return new IfElseInsnTree(this.condition, this.trueBody.asStatement(), this.falseBody.asStatement(), TypeInfos.VOID);
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree trueBody = this.trueBody.cast(parser, type, mode);
		if (trueBody == null) return null;
		InsnTree falseBody = this.falseBody.cast(parser, type, mode);
		if (falseBody == null) return null;
		return new IfElseInsnTree(this.condition, trueBody, falseBody, type);
	}
}