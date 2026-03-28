package builderb0y.bigglobe.structures.megaTree;

import org.joml.Vector3d;

public record Ball(double x, double y, double z, double radius) {

	public Vector3d position() {
		return new Vector3d(this.x, this.y, this.z);
	}
}