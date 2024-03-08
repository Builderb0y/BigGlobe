package builderb0y.bigglobe.compat.voxy;

import java.io.*;

import builderb0y.bigglobe.compat.voxy.DistanceGraph.LeafNode;
import builderb0y.bigglobe.compat.voxy.DistanceGraph.PartialNode;

public class DistanceGraphIO {

	public static final byte CURRENT_VERSION = 1;

	public static void write(DistanceGraph graph, BitOutputStream stream) throws IOException {
		stream.source.writeByte(CURRENT_VERSION);
		writeRecursive(graph.root, stream);
	}

	public static void writeRecursive(DistanceGraph.Node node, BitOutputStream stream) throws IOException {
		if (node instanceof LeafNode leaf) {
			if (leaf.midX != leaf.minX || leaf.midZ != leaf.minZ) { //area is bigger than 1x1.
				stream.write(false);
			}
			stream.write(leaf.full);
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
		int version = stream.source.readUnsignedByte();
		return new DistanceGraph(
			switch (version) {
				case 0 -> readRecursiveV0(minX, maxX, minZ, maxZ, stream);
				case 1 -> readRecursiveV1(minX, maxX, minZ, maxZ, stream);
				default -> throw new IOException("Unknown data version: " + version);
			}
		);
	}

	public static DistanceGraph.Node readRecursiveV0(int minX, int maxX, int minZ, int maxZ, BitInputStream stream) throws IOException {
		if (stream.readBit()) {
			PartialNode partial = new PartialNode(minX, maxX, minZ, maxZ);
			partial.node00 = readRecursiveV0(minX, partial.midX, minZ, partial.midZ, stream);
			partial.node01 = readRecursiveV0(minX, partial.midX, partial.midZ, maxZ, stream);
			partial.node10 = readRecursiveV0(partial.midX, maxX, minZ, partial.midZ, stream);
			partial.node11 = readRecursiveV0(partial.midX, maxX, partial.midZ, maxZ, stream);
			return partial;
		}
		else {
			LeafNode leaf = new LeafNode(minX, maxX, minZ, maxZ);
			leaf.full = stream.readBit();
			return leaf;
		}
	}

	public static DistanceGraph.Node readRecursiveV1(int minX, int maxX, int minZ, int maxZ, BitInputStream stream) throws IOException {
		if (maxX != minX + 1 && maxZ != minZ + 1 && stream.readBit()) {
			PartialNode partial = new PartialNode(minX, maxX, minZ, maxZ);
			partial.node00 = readRecursiveV1(minX, partial.midX, minZ, partial.midZ, stream);
			partial.node01 = readRecursiveV1(minX, partial.midX, partial.midZ, maxZ, stream);
			partial.node10 = readRecursiveV1(partial.midX, maxX, minZ, partial.midZ, stream);
			partial.node11 = readRecursiveV1(partial.midX, maxX, partial.midZ, maxZ, stream);
			return partial;
		}
		else {
			LeafNode leaf = new LeafNode(minX, maxX, minZ, maxZ);
			leaf.full = stream.readBit();
			return leaf;
		}
	}
}