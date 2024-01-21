package builderb0y.scripting.bytecode.tree;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeInfos;

/**
part of method declaration logic needs to be delayed in
order for local variable capturing to work correctly.
specifically, the method needs to be converted to
bytecode after {@link VariableDeclarationInsnTree}
updates all the captured variables' indexes.
*/
public class MethodDeclarationInsnTree implements InsnTree {

	public int access;
	public String name;
	public TypeInfo returnType;
	public LazyVarInfo[] parameters;
	public InsnTree body;

	public MethodDeclarationInsnTree(int access, String name, TypeInfo returnType, LazyVarInfo[] parameters, InsnTree body) {
		this.access = access;
		this.name = name;
		this.returnType = returnType;
		this.parameters = parameters;
		this.body = body;
	}

	public MethodInfo createMethodInfo(TypeInfo owner) {
		return new MethodInfo(
			this.access,
			owner,
			this.name,
			this.returnType,
			CollectionTransformer.convertArray(this.parameters, TypeInfo.ARRAY_FACTORY, LazyVarInfo::type)
		);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		MethodCompileContext newMethod = method.clazz.newMethod(this.access, this.name, this.returnType, this.parameters);
		this.body.emitBytecode(newMethod);
		newMethod.scopes.popScope();
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}
}