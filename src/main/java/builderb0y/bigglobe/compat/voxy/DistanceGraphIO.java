package builderb0y.bigglobe.compat.voxy;

import java.io.IOException;

import builderb0y.bigglobe.compat.voxy.DistanceGraph.LeafNode;
import builderb0y.bigglobe.compat.voxy.DistanceGraph.PartialNode;

public class DistanceGraphIO {

	public static void write(DistanceGraph graph, BitOutputStream stream) throws IOException {
		writeRecursive(graph.root, stream);
	}

	public static void writeRecursive(DistanceGraph.Node node, BitOutputStream stream) throws IOException {
		if (node instanceof LeafNode leaf) {
			stream.append(false).write(leaf.full);
		}
		else {
			DistanceGraph.PartialNode partial = (PartialNode)(node);
			stream.write(true);
			writeRecursive(partial.node00, stream);
			writeRecursive(partial.node01, stream);
			writeRecursive(partial.node10, stream);
			writeRecursive(partial.node11, stream);
		}
	}

	public static DistanceGraph read(int minX, int minZ, int maxX, int maxZ, BitInputStream stream) throws IOException {
		return new DistanceGraph(readRecursive(minX, maxX, minZ, maxZ, stream));
	}

	public static DistanceGraph.Node readRecursive(int minX, int maxX, int minZ, int maxZ, BitInputStream stream) throws IOException {
		if (stream.readBit()) {
			PartialNode partial = new PartialNode(minX, maxX, minZ, maxZ);
			partial.node00 = readRecursive(minX, partial.midX, minZ, partial.midZ, stream);
			partial.node01 = readRecursive(minX, partial.midX, partial.midZ, maxZ, stream);
			partial.node10 = readRecursive(partial.midX, maxX, minZ, partial.midZ, stream);
			partial.node11 = readRecursive(partial.midX, maxX, partial.midZ, maxZ, stream);
			return partial;
		}
		else {
			LeafNode leaf = new LeafNode(minX, maxX, minZ, maxZ);
			leaf.full = stream.readBit();
			return leaf;
		}
	}
}