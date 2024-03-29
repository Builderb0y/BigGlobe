package builderb0y.scripting.bytecode.tree.flow.loop;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForInsnTree implements InsnTree {

	public LoopName loopName;
	public InsnTree initializer;
	public ConditionTree condition;
	public InsnTree step;
	public InsnTree body;

	public ForInsnTree(LoopName loopName, InsnTree initializer, ConditionTree condition, InsnTree step, InsnTree body) {
		this.loopName = loopName;
		this.initializer = initializer.asStatement();
		this.condition = condition;
		this.step = step.asStatement();
		this.body = body.asStatement();
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		LabelNode continuePoint = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.initializer.emitBytecode(method);
		Label startingPoint = label();
		method.node.visitLabel(startingPoint);
		this.condition.emitBytecode(method, null, scope.end.getLabel());
		this.body.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		this.step.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, startingPoint);
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