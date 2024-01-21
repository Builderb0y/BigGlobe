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
	public List<LoadInsnTree> implicitParameters;

	public VariableCapturer(ExpressionParser parser) {
		this.parser = parser;
		this.implicitParameters = new ArrayList<>(16);
	}

	public void addBuiltinParameters() {
		TypeInfo[] types = this.parser.method.info.paramTypes;
		List<ParameterNode> parameters = this.parser.method.node.parameters;
		int offset = this.parser.method.info.isStatic() ? 0 : 1;
		for (int index = offset, size = parameters.size(); index < size; index++) {
			this.implicitParameters.add(load(parameters.get(index).name, types[index - offset]));
		}
	}

	public void addCapturedParameters() {
		for (PendingLocal value : this.parser.environment.user().variables.values()) {
			if (value.assigned) {
				this.implicitParameters.add(value.loader());
			}
		}
	}

	public Stream<TypeInfo> streamImplicitParameterTypes() {
		return (
			this
			.implicitParameters
			.stream()
			.map(LoadInsnTree::variable)
			.map(LazyVarInfo::type)
		);
	}

	public Stream<LazyVarInfo> streamImplicitParameters() {
		return (
			this
			.implicitParameters
			.stream()
			.map(LoadInsnTree::variable)
		);
	}
}