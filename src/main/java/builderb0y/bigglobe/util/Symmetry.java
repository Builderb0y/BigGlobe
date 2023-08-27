package builderb0y.bigglobe.util;

import java.util.random.RandomGenerator;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;

import builderb0y.bigglobe.math.BigGlobeMath;

/**
note: these symmetries assume that the positive z axis is a *clockwise* 90° rotation
from the positive x axis, not a counter-clockwise 90° rotation as one would expect.
this is because minecraft's coordinate system has z oriented this way.
*/
public enum Symmetry {
	IDENTITY {
		@Override public int    getX(int    x, int    z) { return  x; }
		@Override public int    getZ(int    x, int    z) { return  z; }
		@Override public double getX(double x, double z) { return  x; }
		@Override public double getZ(double x, double z) { return  z; }

		@Override
		public BlockState apply(BlockState state) {
			return state;
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return after;
		}

		@Override
		public Symmetry compose(Symmetry before) {
			return before;
		}

		@Override
		public int bulkAndThen(int after) {
			return after;
		}

		@Override
		public int bulkCompose(int before) {
			return before;
		}
	},
	ROTATE_90 {
		@Override public int    getX(int    x, int    z) { return -z; }
		@Override public int    getZ(int    x, int    z) { return  x; }
		@Override public double getX(double x, double z) { return -z; }
		@Override public double getZ(double x, double z) { return  x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_90);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> ROTATE_90;
				case ROTATE_90  -> ROTATE_180;
				case ROTATE_180 -> ROTATE_270;
				case ROTATE_270 -> IDENTITY;
				case FLIP_0     -> FLIP_45;
				case FLIP_45    -> FLIP_90;
				case FLIP_90    -> FLIP_135;
				case FLIP_135   -> FLIP_0;
			};
		}
	},
	ROTATE_180 {
		@Override public int    getX(int    x, int    z) { return -x; }
		@Override public int    getZ(int    x, int    z) { return -z; }
		@Override public double getX(double x, double z) { return -x; }
		@Override public double getZ(double x, double z) { return -z; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_180);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> ROTATE_180;
				case ROTATE_90  -> ROTATE_270;
				case ROTATE_180 -> IDENTITY;
				case ROTATE_270 -> ROTATE_90;
				case FLIP_0     -> FLIP_90;
				case FLIP_45    -> FLIP_135;
				case FLIP_90    -> FLIP_0;
				case FLIP_135   -> FLIP_45;
			};
		}
	},
	ROTATE_270 {
		@Override public int    getX(int    x, int    z) { return  z; }
		@Override public int    getZ(int    x, int    z) { return -x; }
		@Override public double getX(double x, double z) { return  z; }
		@Override public double getZ(double x, double z) { return -x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.COUNTERCLOCKWISE_90);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> ROTATE_270;
				case ROTATE_90  -> IDENTITY;
				case ROTATE_180 -> ROTATE_90;
				case ROTATE_270 -> ROTATE_180;
				case FLIP_0     -> FLIP_135;
				case FLIP_45    -> FLIP_0;
				case FLIP_90    -> FLIP_45;
				case FLIP_135   -> FLIP_90;
			};
		}
	},
	FLIP_0 {
		@Override public int    getX(int    x, int    z) { return -x; }
		@Override public int    getZ(int    x, int    z) { return  z; }
		@Override public double getX(double x, double z) { return -x; }
		@Override public double getZ(double x, double z) { return  z; }

		@Override
		public BlockState apply(BlockState state) {
			return state.mirror(BlockMirror.FRONT_BACK);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> FLIP_0;
				case ROTATE_90  -> FLIP_135;
				case ROTATE_180 -> FLIP_90;
				case ROTATE_270 -> FLIP_45;
				case FLIP_0     -> IDENTITY;
				case FLIP_45    -> ROTATE_270;
				case FLIP_90    -> ROTATE_180;
				case FLIP_135   -> ROTATE_90;
			};
		}
	},
	FLIP_45 {
		@Override public int    getX(int    x, int    z) { return  z; }
		@Override public int    getZ(int    x, int    z) { return  x; }
		@Override public double getX(double x, double z) { return  z; }
		@Override public double getZ(double x, double z) { return  x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_90).mirror(BlockMirror.FRONT_BACK);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> FLIP_45;
				case ROTATE_90  -> FLIP_0;
				case ROTATE_180 -> FLIP_135;
				case ROTATE_270 -> FLIP_90;
				case FLIP_0     -> ROTATE_90;
				case FLIP_45    -> IDENTITY;
				case FLIP_90    -> ROTATE_270;
				case FLIP_135   -> ROTATE_180;
			};
		}
	},
	FLIP_90 {
		@Override public int    getX(int    x, int    z) { return  x; }
		@Override public int    getZ(int    x, int    z) { return -z; }
		@Override public double getX(double x, double z) { return  x; }
		@Override public double getZ(double x, double z) { return -z; }

		@Override
		public BlockState apply(BlockState state) {
			return state.mirror(BlockMirror.LEFT_RIGHT);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> FLIP_90;
				case ROTATE_90  -> FLIP_45;
				case ROTATE_180 -> FLIP_0;
				case ROTATE_270 -> FLIP_135;
				case FLIP_0     -> ROTATE_180;
				case FLIP_45    -> ROTATE_90;
				case FLIP_90    -> IDENTITY;
				case FLIP_135   -> ROTATE_270;
			};
		}
	},
	FLIP_135 {
		@Override public int    getX(int    x, int    z) { return -z; }
		@Override public int    getZ(int    x, int    z) { return -x; }
		@Override public double getX(double x, double z) { return -z; }
		@Override public double getZ(double x, double z) { return -x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_90).mirror(BlockMirror.LEFT_RIGHT);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY   -> FLIP_135;
				case ROTATE_90  -> FLIP_90;
				case ROTATE_180 -> FLIP_45;
				case ROTATE_270 -> FLIP_0;
				case FLIP_0     -> ROTATE_270;
				case FLIP_45    -> ROTATE_180;
				case FLIP_90    -> ROTATE_90;
				case FLIP_135   -> IDENTITY;
			};
		}
	};

	public byte[] bulkAndThen, bulkCompose;

	public static final Symmetry[] VALUES = values();

	public static Symmetry of(BlockRotation rotation) {
		return switch (rotation) {
			case NONE                -> IDENTITY;
			case CLOCKWISE_90        -> ROTATE_90;
			case CLOCKWISE_180       -> ROTATE_180;
			case COUNTERCLOCKWISE_90 -> ROTATE_270;
		};
	}

	public static Symmetry of(BlockMirror mirror) {
		return switch (mirror) {
			case NONE       -> IDENTITY;
			case LEFT_RIGHT -> FLIP_90;
			case FRONT_BACK -> FLIP_0;
		};
	}

	public static Symmetry rotation(int degrees) {
		return switch (BigGlobeMath.modulus_BP(degrees, 360)) {
			case 90  -> ROTATE_90;
			case 180 -> ROTATE_180;
			case 270 -> ROTATE_270;
			default  -> IDENTITY;
		};
	}

	public static Symmetry randomRotation(RandomGenerator random) {
		return VALUES[random.nextInt(4)];
	}

	public static Symmetry flip(int degrees) {
		return switch (BigGlobeMath.modulus_BP(degrees, 180)) {
			case 0   -> FLIP_0;
			case 45  -> FLIP_45;
			case 90  -> FLIP_90;
			case 135 -> FLIP_135;
			default  -> IDENTITY;
		};
	}

	public static Symmetry randomFlip(RandomGenerator random) {
		return VALUES[random.nextInt(4, 8)];
	}

	public static Symmetry randomRotationAndFlip(RandomGenerator random) {
		return VALUES[random.nextInt(8)];
	}

	public boolean isFlipped() {
		return this.ordinal() >= 4;
	}

	public abstract int getX(int x, int z);

	public abstract int getZ(int x, int z);

	public abstract double getX(double x, double z);

	public abstract double getZ(double x, double z);

	public abstract BlockState apply(BlockState state);

	public abstract Symmetry andThen(Symmetry after);

	public Symmetry compose(Symmetry before) {
		return before.andThen(this);
	}

	public Symmetry inverse() {
		return switch (this) {
			case IDENTITY   -> IDENTITY;
			case ROTATE_90  -> ROTATE_270;
			case ROTATE_180 -> ROTATE_180;
			case ROTATE_270 -> ROTATE_90;
			case FLIP_0     -> FLIP_0;
			case FLIP_45    -> FLIP_45;
			case FLIP_90    -> FLIP_90;
			case FLIP_135   -> FLIP_135;
		};
	}

	public int bulkAndThen(int after) {
		if (after == 0 || after == 255) return after;
		if (this.bulkAndThen == null) {
			this.bulkAndThen = new byte[256];
		}
		int output = this.bulkAndThen[after];
		if (output == 0) {
			for (int index = 0; index < 8; index++) {
				if ((after & (1 << index)) != 0) {
					output |= this.andThen(VALUES[index]).flag();
				}
			}
			this.bulkAndThen[after] = (byte)(output);
		}
		else {
			output &= 255;
		}
		return output;
	}

	public int bulkCompose(int before) {
		if (before == 0 || before == 255) return before;
		if (this.bulkCompose == null) {
			this.bulkCompose = new byte[256];
		}
		int output = this.bulkCompose[before];
		if (output == 0) {
			for (int index = 0; index < 8; index++) {
				if ((before & (1 << index)) != 0) {
					output |= VALUES[index].andThen(this).flag();
				}
			}
			this.bulkCompose[before] = (byte)(output);
		}
		else {
			output &= 255;
		}
		return output;
	}

	public int flag() {
		return 1 << this.ordinal();
	}
}