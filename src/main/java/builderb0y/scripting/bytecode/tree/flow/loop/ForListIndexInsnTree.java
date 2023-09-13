package builderb0y.scripting.bytecode.tree.flow.loop;

import java.util.List;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForListIndexInsnTree extends AbstractForIteratorInsnTree {

	public static final MethodInfo
		LIST_SIZE = MethodInfo.getMethod(List.class, "size"),
		LIST_GET = MethodInfo.getMethod(List.class, "get");

	public VariableDeclarationInsnTree indexVariable, elementVariable;
	public VariableDeclareAssignInsnTree list;
	public InsnTree body;

	public ForListIndexInsnTree(
		String loopName,
		VariableDeclarationInsnTree indexVariable,
		VariableDeclarationInsnTree elementVariable,
		VariableDeclareAssignInsnTree list,
		InsnTree body
	) {
		super(loopName);
		this.indexVariable = indexVariable;
		this.elementVariable = elementVariable;
		this.list = list;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//(
		//	List list = this.list
		//	int size = list.size()
		//	int indexVar = 0
		//	Type elementVar
		//	while (indexVar < size:
		//		elementVar = list.get(indexVar)
		//		body
		//		++indexVar
		//	)
		//)
		//
		//begin:
		//	List list = this.list
		//	int size = list.size()
		//	int indexVar = 0
		//	Type elementVar
		//restart:
		//	unless (indexVar < size: goto(end))
		//	elementVar = list.get(indexVar)
		//	body
		//continuePoint:
		//	++indexVar
		//	goto(continuePoint)
		//end:
		LabelNode continuePoint = labelNode(), restart = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.list.emitBytecode(method);
		VarInfo sizeVariable = method.newVariable("size", TypeInfos.INT);
		this.list.variable.emitLoad(method);
		LIST_SIZE.emitBytecode(method);
		sizeVariable.emitStore(method);
		this.indexVariable.emitBytecode(method);
		method.node.visitInsn(ICONST_0);
		this.indexVariable.variable.emitStore(method);
		this.elementVariable.emitBytecode(method);
		method.node.instructions.add(restart);
		this.indexVariable.variable.emitLoad(method);
		sizeVariable.emitLoad(method);
		method.node.visitJumpInsn(IF_ICMPGE, scope.end.getLabel());
		this.list.variable.emitLoad(method);
		this.indexVariable.variable.emitLoad(method);
		LIST_GET.emitBytecode(method);
		castAndStore(this.elementVariable, method);
		this.body.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		method.node.visitIincInsn(this.indexVariable.variable.index, 1);
		method.node.visitJumpInsn(GOTO, restart.getLabel());

		method.scopes.popScope();
	}
}