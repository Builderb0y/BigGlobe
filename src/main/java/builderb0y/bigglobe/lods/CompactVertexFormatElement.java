package builderb0y.bigglobe.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface CompactVertexFormatElement {

	public default void put1f(long pointer, float v0) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put2f(long pointer, float v0, float v1) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put3f(long pointer, float v0, float v1, float v2) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put4f(long pointer, float v0, float v1, float v2, float v3) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put1i(long pointer, int v0) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put2i(long pointer, int v0, int v1) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put3i(long pointer, int v0, int v1, int v2) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public default void put4i(long pointer, int v0, int v1, int v2, int v3) {
		throw new UnsupportedOperationException("Incorrect parameters for " + this);
	}

	public static abstract class Named implements CompactVertexFormatElement {

		public final String name;

		public Named(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}