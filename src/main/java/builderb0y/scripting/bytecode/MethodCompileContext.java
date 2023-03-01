package builderb0y.scripting.bytecode;

import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.bytecode.ScopeContext.Scope;

public class MethodCompileContext {

	public ClassCompileContext clazz;
	public MethodNode node;
	public MethodInfo info;
	public ScopeContext scopes;
	public Map<String, VarInfo> parameters;

	public MethodCompileContext(ClassCompileContext clazz, MethodNode node, MethodInfo info) {
		this.clazz = clazz;
		this.node = node;
		this.info = info;
		this.scopes = new ScopeContext(this);
		this.parameters = new LinkedHashMap<>(5);
	}

	public int nextLocalVariableIndex() {
		int index = 0;
		for (LocalVariableNode variable : this.node.localVariables) {
			index += Type.getType(variable.desc).getSize();
		}
		return index;
	}

	public VarInfo newVariable(String name, TypeInfo type) {
		int index = this.nextLocalVariableIndex();
		Scope scope = this.scopes.peekScope();
		this.node.localVariables.add(new LocalVariableNode(
			name, type.getDescriptor(), null, scope.start(), scope.end(), index
		));
		return new VarInfo(name, index, type);
	}

	public VarInfo newParameter(String name, TypeInfo type) {
		int index = this.nextLocalVariableIndex();
		Scope scope = this.scopes.globalScope();
		this.node.localVariables.add(new LocalVariableNode(
			name, type.getDescriptor(), null, scope.start(), scope.end(), index
		));
		this.node.visitParameter(name, 0);
		VarInfo info = new VarInfo(name, index, type);
		this.parameters.put(name, info);
		return info;
	}

	public VarInfo addThis() {
		return this.newVariable("this", this.clazz.info);
	}

	public VarInfo findParameter(String name) {
		return this.parameters.get(name);
	}
}