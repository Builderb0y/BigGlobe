package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForInsnTree implements InsnTree {

	public String loopName;
	public InsnTree initializer;
	public ConditionTree condition;
	public InsnTree step;
	public InsnTree body;

	public ForInsnTree(String loopName, InsnTree initializer, ConditionTree condition, InsnTree step, InsnTree body) {
		this.loopName = loopName;
		this.initializer = initializer.asStatement();
		this.condition = condition;
		this.step = step.asStatement();
		this.body = body.asStatement();
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		method.scopes.pushScope();
		LabelNode continuePoint = labelNode();
		this.initializer.emitBytecode(method);
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.condition.emitBytecode(method, null, scope.end.getLabel());
		this.body.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		this.step.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, scope.start.getLabel());
		method.scopes.popScope();
		method.scopes.popScope();
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