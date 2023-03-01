package builderb0y.scripting.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.tree.LabelNode;

public class ScopeContext {

	public MethodCompileContext method;
	public List<Scope> currentScope;

	public ScopeContext(MethodCompileContext method) {
		this.method = method;
		this.currentScope = new ArrayList<>(8);
	}

	public void withScope(Consumer<MethodCompileContext> action) {
		this.pushScope();
		action.accept(this.method);
		this.popScope();
	}

	public void pushScope() {
		Scope scope = new Scope();
		this.currentScope.add(scope);
		this.method.node.instructions.add(scope.start);
	}

	public void popScope() {
		Scope scope = this.currentScope.remove(this.currentScope.size() - 1);
		this.method.node.instructions.add(scope.end);
	}

	public Scope peekScope() {
		return this.currentScope.get(this.currentScope.size() - 1);
	}

	public Scope globalScope() {
		return this.currentScope.get(0);
	}

	public record Scope(LabelNode start, LabelNode end) {

		public Scope() {
			this(new LabelNode(), new LabelNode());
		}
	}
}