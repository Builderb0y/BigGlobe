package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CyclicDependencyAnalyzerTest {

	@Test
	public void testGraph0() {
		Node a = new Node("A", true);
		a.connections.add(a);
		checkResult(false, a);
	}

	@Test
	public void testGraph1() {
		Node a = new Node("A", false);
		a.connections.add(a);
		checkResult(true, a);
	}

	@Test
	public void testGraph2() {
		Node
			a = new Node("A", true),
			b = new Node("B", false);
		a.connections.add(b);
		b.connections.add(b);
		checkResult(true, a, b);
	}

	@Test
	public void testGraph3() {
		Node
			a = new Node("A", true),
			b = new Node("B", false);
		a.connections.add(b);
		b.connections.add(a);
		checkResult(false, a, b);
	}

	@Test
	public void testGraph4() {
		Node
			a = new Node("A", false),
			b = new Node("B", false),
			c = new Node("C", true),
			d = new Node("D", true);
		a.connections.add(b);
		b.connections.add(a);
		b.connections.add(c);
		c.connections.add(d);
		d.connections.add(a);
		checkResult(false, a, b, c, d);
	}

	@Test
	public void testGraph5() {
		Node
			a = new Node("A", false),
			b = new Node("B", false),
			c = new Node("C", true),
			d = new Node("D", true);
		a.connections.add(b);
		b.connections.add(a);
		b.connections.add(c);
		c.connections.add(d);
		checkResult(true, a, b, c, d);
	}

	public static void checkResult(boolean expectSuccess, Node... nodes) {
		checkResultRecursive(expectSuccess, nodes, 0);
	}

	public static void checkResultRecursive(boolean expectSuccess, Node[] nodes, int start) {
		if (start < nodes.length) {
			for (int other = start; other < nodes.length; other++) {
				swap(nodes, start, other);
				checkResultRecursive(expectSuccess, nodes, start + 1);
				swap(nodes, start, other);
			}
		}
		else {
			checkResultNonRecursive(expectSuccess, nodes);
		}
	}

	public static void swap(Node[] nodes, int index1, int index2) {
		Node tmp = nodes[index1];
		nodes[index1] = nodes[index2];
		nodes[index2] = tmp;
	}

	public static void checkResultNonRecursive(boolean expectSuccess, Node[] nodes) {
		Analyzer analyzer = new Analyzer();
		try {
			for (Node node : nodes) {
				analyzer.accept(node);
			}
			if (!expectSuccess) {
				fail(Arrays.toString(nodes));
			}
		}
		catch (CyclicDependencyException exception) {
			if (expectSuccess) {
				fail(exception);
			}
		}
	}

	public static class Analyzer extends GenericCyclicDependencyAnalyzer<Node> {

		@Override
		public Stream<? extends Node> getDependencies(Node object) {
			return object.connections.stream();
		}

		@Override
		public boolean areCyclesFatal(Node object) {
			return object.cyclesAreFatal;
		}
	}

	public static class Node {

		public final String name;
		public final Set<Node> connections;
		public final boolean cyclesAreFatal;

		public Node(String name, boolean cyclesAreFatal) {
			this.name = name;
			this.connections = new HashSet<>();
			this.cyclesAreFatal = cyclesAreFatal;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}