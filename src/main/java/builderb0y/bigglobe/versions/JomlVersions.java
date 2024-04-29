package builderb0y.bigglobe.versions;

import java.nio.FloatBuffer;

import org.joml.*;

#if MC_VERSION <= MC_1_19_2
import net.minecraft.util.math.Vec3f;
#endif

public class JomlVersions {

	public static final FloatBuffer MATRIX_SCRATCH = FloatBuffer.allocate(16);

	public static Vector3f copy(Vector3f src, Vector3f dst) {
		return dst.set(src);
	}

	#if MC_VERSION <= MC_1_19_2
		public static Vector3f copy(Vec3f src, Vector3f dst) {
			return dst.set(src.getX(), src.getY(), src.getZ());
		}

		public static Vec3f copy(Vector3f src, Vec3f dst) {
			dst.set(src.x, src.y, src.z);
			return dst;
		}

		public static Vec3f copy(Vec3f src, Vec3f dst) {
			dst.set(src);
			return dst;
		}
	#endif

	public static Vector4f copy(Vector4f src, Vector4f dst) {
		return dst.set(src);
	}

	#if MC_VERSION <= MC_1_19_2
		public static Vector4f copy(net.minecraft.util.math.Vector4f src, Vector4f dst) {
			return dst.set(src.getX(), src.getY(), src.getZ());
		}

		public static net.minecraft.util.math.Vector4f copy(Vector4f src, net.minecraft.util.math.Vector4f dst) {
			dst.set(src.x, src.y, src.z, src.w);
			return dst;
		}

		public static net.minecraft.util.math.Vector4f copy(net.minecraft.util.math.Vector4f src, net.minecraft.util.math.Vector4f dst) {
			dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
			return dst;
		}
	#endif

	public static Matrix4f copy(Matrix4f src, Matrix4f dst) {
		return dst.set(src);
	}

	#if MC_VERSION <= MC_1_19_2
		public static synchronized Matrix4f copy(net.minecraft.util.math.Matrix4f src, Matrix4f dst) {
			src.writeColumnMajor(MATRIX_SCRATCH);
			return dst.set(MATRIX_SCRATCH);
		}

		public static synchronized net.minecraft.util.math.Matrix4f copy(Matrix4f src, net.minecraft.util.math.Matrix4f dst) {
			src.get(MATRIX_SCRATCH);
			dst.readColumnMajor(MATRIX_SCRATCH);
			return dst;
		}

		public static net.minecraft.util.math.Matrix4f copy(net.minecraft.util.math.Matrix4f src, net.minecraft.util.math.Matrix4f dst) {
			dst.load(src);
			return dst;
		}
	#endif

	public static Vector3f coerce(Vector3f target, Vector3f scratch) {
		return target;
	}

	#if MC_VERSION <= MC_1_19_2
		public static Vector3f coerce(Vec3f target, Vector3f scratch) {
			return scratch.set(target.getX(), target.getY(), target.getZ());
		}

		public static Vec3f coerce(Vector3f target, Vec3f scratch) {
			scratch.set(target.x, target.y, target.z);
			return scratch;
		}

		public static Vec3f coerce(Vec3f target, Vec3f scratch) {
			return target;
		}
	#endif

	public static Vector4f coerce(Vector4f target, Vector4f scratch) {
		return target;
	}

	#if MC_VERSION <= MC_1_19_2
		public static Vector4f coerce(net.minecraft.util.math.Vector4f target, Vector4f scratch) {
			return scratch.set(target.getX(), target.getY(), target.getZ());
		}

		public static net.minecraft.util.math.Vector4f coerce(Vector4f target, net.minecraft.util.math.Vector4f scratch) {
			scratch.set(target.x, target.y, target.z, target.w);
			return scratch;
		}

		public static net.minecraft.util.math.Vector4f coerce(net.minecraft.util.math.Vector4f target, net.minecraft.util.math.Vector4f scratch) {
			return target;
		}
	#endif

	public static Matrix4f coerce(Matrix4f target, Matrix4f scratch) {
		return target;
	}

	#if MC_VERSION <= MC_1_19_2
		public static synchronized Matrix4f coerce(net.minecraft.util.math.Matrix4f target, Matrix4f scratch) {
			target.writeColumnMajor(MATRIX_SCRATCH);
			return scratch.set(MATRIX_SCRATCH);
		}

		public static synchronized net.minecraft.util.math.Matrix4f coerce(Matrix4f target, net.minecraft.util.math.Matrix4f scratch) {
			target.get(MATRIX_SCRATCH);
			scratch.readColumnMajor(MATRIX_SCRATCH);
			return scratch;
		}

		public static net.minecraft.util.math.Matrix4f coerce(net.minecraft.util.math.Matrix4f target, net.minecraft.util.math.Matrix4f scratch) {
			return target;
		}
	#endif
}