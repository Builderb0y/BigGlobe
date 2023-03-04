package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.MethodCompileContext;
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
	/**
	runtime bodies are guaranteed to have the same {@link InsnTree#getTypeInfo()}.
	compile bodies are not. the runtime bodies will be used for emitting bytecode,
	and the compile bodies will be used for casting {@link #cast(ExpressionParser, TypeInfo, CastMode)}.
	*/
	public final InsnTree compileTrueBody, compileFalseBody, runtimeTrueBody, runtimeFalseBody;
	public final TypeInfo type;

	public IfElseInsnTree(
		ConditionTree condition,
		InsnTree compileTrueBody,
		InsnTree compileFalseBody,
		InsnTree runtimeTrueBody,
		InsnTree runtimeFalseBody,
		TypeInfo type
	) {
		this.condition = condition;
		this.compileTrueBody = compileTrueBody;
		this.compileFalseBody = compileFalseBody;
		this.runtimeTrueBody = runtimeTrueBody;
		this.runtimeFalseBody = runtimeFalseBody;
		this.type = type;
	}

	public static InsnTree create(ExpressionParser parser, ConditionTree condition, InsnTree trueBody, InsnTree falseBody) throws ScriptParsingException {
		InsnTree runtimeTrueBody = trueBody;
		InsnTree runtimeFalseBody = falseBody;
		TypeInfo type;
		if (trueBody.returnsUnconditionally()) {
			if (falseBody.returnsUnconditionally()) {
				type = TypeInfos.VOID;
			}
			else {
				type = falseBody.getTypeInfo();
			}
		}
		else {
			if (falseBody.returnsUnconditionally()) {
				type = trueBody.getTypeInfo();
			}
			else {
				type = TypeMerger.computeMostSpecificType(trueBody.getTypeInfo(), falseBody.getTypeInfo());
				runtimeTrueBody  = trueBody .cast(parser, type, CastMode.IMPLICIT_THROW);
				runtimeFalseBody = falseBody.cast(parser, type, CastMode.IMPLICIT_THROW);
			}
		}
		return new IfElseInsnTree(condition, trueBody, falseBody, runtimeTrueBody, runtimeFalseBody, type);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label falseLabel = new Label(), end = new Label();
		method.scopes.withScope(method1 -> {
			this.condition.emitBytecode(method1, null, falseLabel);
			this.runtimeTrueBody.emitBytecode(method1);
		});
		method.node.visitJumpInsn(GOTO, end);
		method.node.visitLabel(falseLabel);
		this.runtimeFalseBody.emitBytecode(method);
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.type;
	}

	@Override
	public boolean returnsUnconditionally() {
		if (this.condition instanceof ConstantConditionTree constant) {
			return (constant.value ? this.compileTrueBody : this.compileFalseBody).returnsUnconditionally();
		}
		else {
			return this.compileTrueBody.returnsUnconditionally() && this.compileFalseBody.returnsUnconditionally();
		}
	}

	@Override
	public boolean canBeStatement() {
		return this.compileTrueBody.canBeStatement() && this.compileFalseBody.canBeStatement();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree trueBody = this.compileTrueBody.cast(parser, type, mode);
		if (trueBody == null) return null;
		InsnTree falseBody = this.compileFalseBody.cast(parser, type, mode);
		if (falseBody == null) return null;
		return new IfElseInsnTree(this.condition, trueBody, falseBody, trueBody, falseBody, type);
	}
}