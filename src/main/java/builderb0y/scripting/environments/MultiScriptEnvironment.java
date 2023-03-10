package builderb0y.scripting.environments;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class MultiScriptEnvironment implements ScriptEnvironment {

	public List<ScriptEnvironment> environments;

	public MultiScriptEnvironment() {
		this.environments = new ArrayList<>(8);
	}

	public MultiScriptEnvironment(MultiScriptEnvironment from) {
		this.environments = new ArrayList<>(from.environments);
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).getVariable(parser, name);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getField(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).getField(parser, receiver, name);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).getFunction(parser, name, arguments);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).getMethod(parser, receiver, name, arguments);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree parseKeyword(ExpressionParser parser, String name) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).parseKeyword(parser, name);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			InsnTree variable = environments.get(index).parseMemberKeyword(parser, receiver, name);
			if (variable != null) return variable;
		}
		return null;
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		List<ScriptEnvironment> environments = this.environments;
		for (int index = 0, size = environments.size(); index < size; index++) {
			TypeInfo type = environments.get(index).getType(parser, name);
			if (type != null) return type;
		}
		return null;
	}

	@Override
	public String toString() {
		return "MultiScriptEnvironment: " + this.environments;
	}
}