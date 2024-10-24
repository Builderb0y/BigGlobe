package builderb0y.scripting.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.objectweb.asm.tree.ParameterNode;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.UserScriptEnvironment.PendingLocal;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VariableCapturer {

	public final ExpressionParser parser;
	public List<LoadInsnTree> builtinParameters, capturedVariables;

	public VariableCapturer(ExpressionParser parser) {
		this.parser = parser;
		this.builtinParameters = new ArrayList<>(8);
		this.capturedVariables = new ArrayList<>(8);
	}

	public void addBuiltinParameters() {
		TypeInfo[] types = this.parser.method.info.paramTypes;
		List<ParameterNode> parameters = this.parser.method.node.parameters;
		for (int index = 0, size = parameters.size(); index < size; index++) {
			this.builtinParameters.add(load(parameters.get(index).name, types[index]));
		}
	}

	public void addCapturedParameters() {
		for (PendingLocal value : this.parser.environment.user().variables.values()) {
			if (value.assigned) {
				this.capturedVariables.add(value.loader());
			}
		}
	}

	public Stream<TypeInfo> streamImplicitParameterTypes() {
		return this.streamImplicitParameters().map(LazyVarInfo::type);
	}

	public Stream<LazyVarInfo> streamImplicitParameters() {
		return (
			Stream.concat(
				this.builtinParameters.stream(),
				this.capturedVariables.stream()
			)
			.map(LoadInsnTree::variable)
		);
	}
}