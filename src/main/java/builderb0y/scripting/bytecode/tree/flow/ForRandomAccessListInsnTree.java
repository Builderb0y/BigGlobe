package builderb0y.scripting.bytecode.tree.flow;

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

public class ForRandomAccessListInsnTree extends AbstractForIteratorInsnTree {

	public static final MethodInfo
		SIZE = MethodInfo.getMethod(List.class, "size"),
		GET  = MethodInfo.getMethod(List.class, "get");

	public VariableDeclarationInsnTree variable;
	public VariableDeclareAssignInsnTree list;
	public InsnTree body;

	public ForRandomAccessListInsnTree(
		String loopName,
		VariableDeclarationInsnTree variable,
		VariableDeclareAssignInsnTree list,
		InsnTree body
	) {
		super(loopName);
		this.variable = variable;
		this.list = list;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//(
		//	Type userVar
		//	List list = userList
		//	for (int index = 0 int size = list.size(), index < size, ++index:
		//		userVar = list.get(index)
		//		body
		//	)
		//)
		//
		//begin:
		//	Type userVar
		//	List list = userList
		//	int index = 0
		//	int size = list.size()
		//continuePoint:
		//	unless (index < size: goto(end))
		//	userVar = list.get(index)
		//	body
		//	++index
		//	goto(continuePoint)
		//end:
		LabelNode continuePoint = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.variable.emitBytecode(method);
		this.list.emitBytecode(method);
		VarInfo index = method.newVariable("index", TypeInfos.INT);
		method.node.visitInsn(ICONST_0);
		index.emitStore(method);
		VarInfo size = method.newVariable("size", TypeInfos.INT);
		this.list.variable.emitLoad(method);
		SIZE.emit(method);
		size.emitStore(method);
		method.node.instructions.add(continuePoint);
		index.emitLoad(method);
		size.emitLoad(method);
		method.node.visitJumpInsn(IF_ICMPGE, scope.end.getLabel());
		this.list.variable.emitLoad(method);
		index.emitLoad(method);
		GET.emit(method);
		castAndstore(this.variable, method);
		this.body.emitBytecode(method);
		method.node.visitIincInsn(index.index, 1);
		method.node.visitJumpInsn(GOTO, continuePoint.getLabel());

		method.scopes.popScope();
	}
}