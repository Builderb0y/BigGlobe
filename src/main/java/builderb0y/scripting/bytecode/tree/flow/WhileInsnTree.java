package builderb0y.scripting.bytecode.tree.flow;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WhileInsnTree implements InsnTree {

	public ConditionTree condition;
	public InsnTree body;

	public WhileInsnTree(ConditionTree condition, InsnTree body) {
		this.condition = condition;
		this.body = body.asStatement();
	}

	public static InsnTree createRepeat(ExpressionParser parser, InsnTree times, InsnTree body) {
		if (!times.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Number of times to repeat is not an int");
		}
		VariableDeclarationInsnTree counter = new VariableDeclarationInsnTree("counter", TypeInfos.INT);
		InsnTree init, loadLimit;
		if (times.getConstantValue().isConstant()) {
			//var counter = 0
			//while (counter < times:
			//	body
			//	++counter
			//)
			init = seq(counter, store(counter.loader.variable, ldc(0)));
			loadLimit = times;
		}
		else {
			//var limit = times
			//var counter = 0
			//while (counter < limit:
			//	body
			//	++counter
			//)
			VariableDeclarationInsnTree limit = new VariableDeclarationInsnTree("limit", TypeInfos.INT);
			init = seq(limit, store(limit.loader.variable, times), counter, store(counter.loader.variable, ldc(0)));
			//init = limit.then(store(limit.loader.variable, times)).then(counter).then(store(counter.loader.variable, ldc(0)));
			loadLimit = limit.loader;
		}
		return for_(init, lt(parser, counter.loader, loadLimit), inc(counter.loader.variable, 1), body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Scope scope = method.scopes.pushLoop();
		this.condition.emitBytecode(method, null, scope.end.getLabel());
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, scope.start.getLabel());
		method.scopes.popLoop();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}