package builderb0y.scripting.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.objectweb.asm.tree.LabelNode;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScopeContext {

	public MethodCompileContext method;
	public List<Scope> stack;

	public ScopeContext(MethodCompileContext method) {
		this.method = method;
		this.stack = new ArrayList<>(8);
	}

	public void withScope(Consumer<MethodCompileContext> action) {
		this.pushScope();
		action.accept(this.method);
		this.popScope();
	}

	public void withLoop(Consumer<MethodCompileContext> action) {
		this.pushLoop();
		action.accept(this.method);
		this.popScope();
	}

	public Scope pushScope() {
		Scope scope = new Scope(Scope.Type.NORMAL);
		this.stack.add(scope);
		this.method.node.instructions.add(scope.start);
		return scope;
	}

	public Scope pushManualScope() {
		Scope scope = new Scope(Scope.Type.MANUAL);
		this.stack.add(scope);
		return scope;
	}

	public Scope pushLoop() {
		Scope scope = new Scope(Scope.Type.LOOP);
		this.stack.add(scope);
		this.method.node.instructions.add(scope.start);
		return scope;
	}

	public Scope pushLoop(LabelNode continuePoint) {
		Scope scope = this.pushLoop();
		scope.continuePoint = continuePoint;
		return scope;
	}

	public void popScope() {
		Scope scope = this.stack.remove(this.stack.size() - 1);
		this.method.node.instructions.add(scope.end);
	}

	public void popLoop() {
		this.popScope();
	}

	public void popManualScope() {
		this.stack.remove(this.stack.size() - 1);
	}

	public Scope peekScope() {
		return this.stack.get(this.stack.size() - 1);
	}

	public Scope globalScope() {
		return this.stack.get(0);
	}

	public Scope findLoop() {
		List<Scope> stack = this.stack;
		for (int index = stack.size(); --index >= 0;) {
			Scope scope = stack.get(index);
			if (scope.type == Scope.Type.LOOP) return scope;
		}
		throw new IllegalStateException("No enclosing loop");
	}

	public static class Scope {

		public LabelNode start;
		public LabelNode end;
		public LabelNode continuePoint;
		public Type type;

		public Scope(Type type) {
			this.type = type;
			if (type != Type.MANUAL) {
				this.start = labelNode();
				this.end = labelNode();
			}
		}

		public void cycle() {
			this.start = this.end;
			this.end = labelNode();
			this.continuePoint = null;
		}

		public LabelNode getContinuePoint() {
			return this.continuePoint != null ? this.continuePoint : this.start;
		}

		public static enum Type {
			NORMAL,
			MANUAL,
			LOOP;
		}
	}
}