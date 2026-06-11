package builderb0y.bigglobe.columns.scripted.dependencies;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.Test;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

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

	@Test
	public void testGraph6() {
		Node
			a = new Node("A", false),
			b = new Node("B", false),
			c = new Node("C", true),
			d = new Node("D", true);
		a.connections.add(b);
		b.connections.add(a);
		b.connections.add(c);
		d.connections.add(a);
		checkResult(true, a, b, c, d);
	}

	@Test
	public void testGraph7() {
		Node
			a = new Node("A", false),
			b = new Node("B", false),
			c = new Node("C", true),
			d = new Node("D", true);
		a.connections.add(b);
		b.connections.add(a);
		c.connections.add(d);
		d.connections.add(a);
		checkResult(true, a, b, c, d);
	}

	@Test
	public void testRandomGraph() {
		int valids = 0, invalids = 0;
		RandomGenerator random = new Permuter(12345L);
		for (int trial = 0; trial < 10000; trial++) {
			int nodeCount = random.nextInt(16) + 1;
			Node[] nodes = new Node[nodeCount];
			for (int index = 0; index < nodeCount; index++) {
				nodes[index] = new Node(String.valueOf((char)(index + 'A')), random.nextBoolean());
			}
			int connections = (int)(BigGlobeMath.squareD(random.nextDouble() * nodeCount));
			for (int connection = 0; connection < connections; connection++) {
				nodes[random.nextInt(nodeCount)].connections.add(nodes[random.nextInt(nodeCount)]);
			}
			boolean expectValid = checkNaive(nodes);
			checkResultNonRecursive(expectValid, nodes);
			if (expectValid) valids++;
			else invalids++;
		}
		System.out.println(valids + " valid, " + invalids + " invalid.");
	}

	public static boolean checkNaive(Node[] nodes) {
		Set<Node> set = new ObjectOpenHashSet<>();
		for (Node node : nodes) {
			if (node.cyclesAreFatal) {
				for (Node connection : node.connections) {
					if (!checkNaive(node, connection, set)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static boolean checkNaive(Node original, Node node, Set<Node> nodes) {
		if (node == original) return false;
		if (nodes.add(node)) try {
			for (Node connection : node.connections) {
				if (!checkNaive(original, connection, nodes)) {
					return false;
				}
			}
		}
		finally {
			nodes.remove(node);
		}
		return true;
	}

	public static void checkResult(boolean expectSuccess, Node... nodes) {
		checkResultRecursive(expectSuccess, nodes, 0);
		assertEquals(expectSuccess, checkNaive(nodes));
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
		//System.out.println("BEGIN: " + Arrays.toString(nodes));
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

		/*
		public int depth = 0;

		@Override
		public void accept(Node node) {
			int depth = this.depth;
			System.out.println("\t".repeat(depth) + node);
			this.depth = depth + 1;
			try {
				super.accept(node);
			}
			finally {
				this.depth = depth;
				System.out.println("\t".repeat(depth) + node + ": " + this.reachable.get(node));
			}
		}
		*/

		@Override
		public Stream<? extends Node> getDependencies(Node object) {
			return object.connections.stream();
		}

		@Override
		public boolean areCyclesFatal(Node node) {
			return node.cyclesAreFatal;
		}
	}

	public static class Node {

		public final String name;
		public final Set<Node> connections;
		public final boolean cyclesAreFatal;

		public Node(String name, boolean cyclesAreFatal) {
			this.name = name;
			this.connections = new LinkedHashSet<>();
			this.cyclesAreFatal = cyclesAreFatal;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}