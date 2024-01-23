package builderb0y.scripting.bytecode;

import java.util.Arrays;

import org.objectweb.asm.tree.MethodNode;

import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class MethodCompileContext {

	public ClassCompileContext clazz;
	public MethodNode node;
	public MethodInfo info;
	public ScopeContext scopes;
	public int mangledVariableCounter;

	public MethodCompileContext(ClassCompileContext clazz, MethodNode node, MethodInfo info, String... parameterNames) {
		if (info.paramTypes.length != parameterNames.length) {
			throw new IllegalArgumentException("Parameter mismatch: expected " + Arrays.toString(info.paramTypes) + ", got " + Arrays.toString(parameterNames));
		}
		this.clazz = clazz;
		this.node = node;
		this.info = info;
		this.scopes = new ScopeContext(this);
		if (!info.isAbstract()) this.scopes.pushScope();
		if (!info.isStatic()) {
			this.node.visitParameter("this", 0);
			if (!info.isAbstract()) this.scopes.addVariable("this", clazz.info);
		}
		for (int index = 0, length = parameterNames.length; index < length; index++) {
			this.node.visitParameter(parameterNames[index], 0);
			if (!info.isAbstract()) this.scopes.addVariable(parameterNames[index], info.paramTypes[index]);
		}
	}

	public void appendCode(String code, MutableScriptEnvironment environment) {
		try {
			new ExpressionParser(code, this.clazz, this).addEnvironment(environment).parseRemainingInput(false, false).emitBytecode(this);
		}
		catch (ScriptParsingException exception) {
			throw new RuntimeException(exception);
		}
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

	public String mangleName(String name) {
		return '$' + name + this.mangledVariableCounter++;
	}
}