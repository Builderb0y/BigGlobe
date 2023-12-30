package builderb0y.scripting.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

	public Scope pushScope() {
		Scope scope = Scope.normal();
		this.stack.add(scope);
		this.method.node.instructions.add(scope.start);
		return scope;
	}

	public Scope pushManualScope() {
		Scope scope = Scope.manual();
		this.stack.add(scope);
		return scope;
	}

	public Scope pushLoop(LoopName loopName) {
		Scope scope = Scope.loop(loopName);
		this.stack.add(scope);
		this.method.node.instructions.add(scope.start);
		return scope;
	}

	public Scope pushLoop(LoopName name, LabelNode continuePoint) {
		Scope scope = this.pushLoop(name);
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

	public Scope findLoopForContinue(String loopName) {
		List<Scope> stack = this.stack;
		for (int index = stack.size(); --index >= 0;) {
			Scope scope = stack.get(index);
			if (scope.type == Scope.Type.LOOP && (loopName == null || loopName.equals(scope.loopName.name))) {
				return scope;
			}
		}
		throw new IllegalStateException(loopName == null ? "No enclosing loop" : "No enclosing loop named " + loopName);
	}

	public Scope findLoopForBreak(String loopName) {
		List<Scope> stack = this.stack;
		for (int index = stack.size(); --index >= 0;) {
			Scope scope = stack.get(index);
			if (scope.type == Scope.Type.LOOP && (loopName == null || loopName.equals(scope.loopName.name))) {
				while (--index >= 0) {
					Scope next = stack.get(index);
					if (next.loopName == scope.loopName) scope = next;
					else break;
				}
				return scope;
			}
		}
		throw new IllegalStateException(loopName == null ? "No enclosing loop" : "No enclosing loop named " + loopName);
	}

	public static class Scope {

		public LabelNode start;
		public LabelNode end;
		public LabelNode continuePoint;
		public Type type;
		public @NotNull LoopName loopName;

		public Scope(
			LabelNode start,
			LabelNode end,
			LabelNode continuePoint,
			Type type,
			@NotNull LoopName loopName
		) {
			this.start = start;
			this.end = end;
			this.continuePoint = continuePoint;
			this.type = type;
			this.loopName = loopName;
		}

		public static Scope normal() {
			return new Scope(labelNode(), labelNode(), null, Type.NORMAL, LoopName.NOT_A_LOOP);
		}

		public static Scope manual() {
			return new Scope(null, null, null, Type.MANUAL, LoopName.NOT_A_LOOP);
		}

		public static Scope loop(LoopName loopName) {
			return new Scope(labelNode(), labelNode(), null, Type.LOOP, loopName);
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

	public static record LoopName(@Nullable String name) {

		public static final LoopName NOT_A_LOOP = new LoopName(null);

		public static LoopName of(String name) {
			return new LoopName(name);
		}
	}
}