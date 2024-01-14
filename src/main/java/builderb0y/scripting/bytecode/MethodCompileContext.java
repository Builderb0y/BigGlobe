package builderb0y.scripting.bytecode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

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

	public VarInfo getParameter(String name) {
		VarInfo parameter = this.parameters.get(name);
		if (parameter != null) return parameter;
		else throw new IllegalStateException("Missing parameter: " + name);
	}

	public MethodCompileContext prepareParameters(String... parameterNames) {
		if (parameterNames.length != this.info.paramTypes.length) {
			throw new IllegalArgumentException("Parameter mismatch: expected " + Arrays.toString(this.info.paramTypes) + ", got " + Arrays.toString(parameterNames));
		}
		this.scopes.pushScope();
		if (!this.info.isStatic()) this.addThis();
		for (int index = 0, length = parameterNames.length; index < length; index++) {
			this.newParameter(parameterNames[index], this.info.paramTypes[index]);
		}
		return this;
	}

	public MethodCompileContext appendCode(String code, MutableScriptEnvironment environment) {
		try {
			new ExpressionParser(code, this.clazz, this).addEnvironment(environment).parseRemainingInput(true, false).emitBytecode(this);
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception);
		}
		return this;
	}

	public void setCode(String code, MutableScriptEnvironment environment) {
		try {
			new ExpressionParser(code, this.clazz, this).addEnvironment(environment).parseEntireInput().emitBytecode(this);
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception);
		}
		this.endCode();
	}

	public void endCode() {
		this.scopes.popScope();
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
			name, type.getDescriptor(), null, scope.start, scope.end, index
		));
		return new VarInfo(name, index, type);
	}

	public VarInfo newParameter(String name, TypeInfo type) {
		int index = this.nextLocalVariableIndex();
		Scope scope = this.scopes.globalScope();
		this.node.localVariables.add(new LocalVariableNode(
			name, type.getDescriptor(), null, scope.start, scope.end, index
		));
		this.node.visitParameter(name, 0);
		VarInfo info = new VarInfo(name, index, type);
		this.parameters.put(name, info);
		return info;
	}

	public VarInfo addThis() {
		return this.newVariable("this", this.clazz.info);
	}
}