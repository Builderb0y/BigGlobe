package builderb0y.bigglobe.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;

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
				case IDENTITY    -> ROTATE_90;
				case ROTATE_90   -> ROTATE_180;
				case ROTATE_180  -> ROTATE_270;
				case ROTATE_270  -> IDENTITY;
				case FLIP_X      -> FLIP_XZ;
				case FLIP_Z      -> FLIP_XZ_INV;
				case FLIP_XZ     -> FLIP_Z;
				case FLIP_XZ_INV -> FLIP_X;
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
				case IDENTITY    -> ROTATE_180;
				case ROTATE_90   -> ROTATE_270;
				case ROTATE_180  -> IDENTITY;
				case ROTATE_270  -> ROTATE_90;
				case FLIP_X      -> FLIP_Z;
				case FLIP_Z      -> FLIP_X;
				case FLIP_XZ     -> FLIP_XZ_INV;
				case FLIP_XZ_INV -> FLIP_XZ;
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
				case IDENTITY    -> ROTATE_270;
				case ROTATE_90   -> IDENTITY;
				case ROTATE_180  -> ROTATE_90;
				case ROTATE_270  -> ROTATE_180;
				case FLIP_X      -> FLIP_XZ_INV;
				case FLIP_Z      -> FLIP_XZ;
				case FLIP_XZ     -> FLIP_X;
				case FLIP_XZ_INV -> FLIP_Z;
			};
		}
	},
	FLIP_X {
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
				case IDENTITY    -> FLIP_X;
				case ROTATE_90   -> FLIP_XZ_INV;
				case ROTATE_180  -> FLIP_Z;
				case ROTATE_270  -> FLIP_XZ;
				case FLIP_X      -> IDENTITY;
				case FLIP_Z      -> ROTATE_180;
				case FLIP_XZ     -> ROTATE_270;
				case FLIP_XZ_INV -> ROTATE_90;
			};
		}
	},
	FLIP_Z {
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
				case IDENTITY    -> FLIP_Z;
				case ROTATE_90   -> FLIP_XZ;
				case ROTATE_180  -> FLIP_X;
				case ROTATE_270  -> FLIP_XZ_INV;
				case FLIP_X      -> ROTATE_180;
				case FLIP_Z      -> IDENTITY;
				case FLIP_XZ     -> ROTATE_90;
				case FLIP_XZ_INV -> ROTATE_270;
			};
		}
	},
	FLIP_XZ {
		@Override public int    getX(int    x, int    z) { return z; }
		@Override public int    getZ(int    x, int    z) { return x; }
		@Override public double getX(double x, double z) { return z; }
		@Override public double getZ(double x, double z) { return x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_90).mirror(BlockMirror.LEFT_RIGHT);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY    -> FLIP_XZ;
				case ROTATE_90   -> FLIP_X;
				case ROTATE_180  -> FLIP_XZ_INV;
				case ROTATE_270  -> FLIP_Z;
				case FLIP_X      -> ROTATE_90;
				case FLIP_Z      -> ROTATE_270;
				case FLIP_XZ     -> IDENTITY;
				case FLIP_XZ_INV -> ROTATE_180;
			};
		}
	},
	FLIP_XZ_INV {
		@Override public int    getX(int    x, int    z) { return -z; }
		@Override public int    getZ(int    x, int    z) { return -x; }
		@Override public double getX(double x, double z) { return -z; }
		@Override public double getZ(double x, double z) { return -x; }

		@Override
		public BlockState apply(BlockState state) {
			return state.rotate(BlockRotation.CLOCKWISE_90).mirror(BlockMirror.FRONT_BACK);
		}

		@Override
		public Symmetry andThen(Symmetry after) {
			return switch (after) {
				case IDENTITY    -> FLIP_XZ_INV;
				case ROTATE_90   -> FLIP_Z;
				case ROTATE_180  -> FLIP_XZ;
				case ROTATE_270  -> FLIP_X;
				case FLIP_X      -> ROTATE_270;
				case FLIP_Z      -> ROTATE_90;
				case FLIP_XZ     -> ROTATE_180;
				case FLIP_XZ_INV -> IDENTITY;
			};
		}
	};

	public byte[] bulkAndThen, bulkCompose;

	public static final Symmetry[] VALUES = values();

	public static Symmetry of(BlockRotation rotation) {
		return switch (rotation) {
			case NONE -> IDENTITY;
			case CLOCKWISE_90 -> ROTATE_90;
			case CLOCKWISE_180 -> ROTATE_180;
			case COUNTERCLOCKWISE_90 -> ROTATE_270;
		};
	}

	public static Symmetry of(BlockMirror mirror) {
		return switch (mirror) {
			case NONE -> IDENTITY;
			case LEFT_RIGHT -> FLIP_X;
			case FRONT_BACK -> FLIP_Z;
		};
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
			case IDENTITY    -> IDENTITY;
			case ROTATE_90   -> ROTATE_270;
			case ROTATE_180  -> ROTATE_180;
			case ROTATE_270  -> ROTATE_90;
			case FLIP_X      -> FLIP_X;
			case FLIP_Z      -> FLIP_Z;
			case FLIP_XZ     -> FLIP_XZ;
			case FLIP_XZ_INV -> FLIP_XZ_INV;
		};
	}

	public int bulkAndThen(int input) {
		if (input == 0) return 0;
		if (this.bulkAndThen == null) {
			this.bulkAndThen = new byte[256];
		}
		int output = this.bulkAndThen[input];
		if (output == 0) {
			for (int index = 0; index < 8; index++) {
				if ((input & (1 << index)) != 0) {
					output |= 1 << VALUES[index].andThen(this).ordinal();
				}
			}
			this.bulkAndThen[input] = (byte)(output);
		}
		else {
			output &= 255;
		}
		return output;
	}

	public int bulkCompose(int input) {
		if (input == 0) return 0;
		if (this.bulkCompose == null) {
			this.bulkCompose = new byte[256];
		}
		int output = this.bulkCompose[input];
		if (output == 0) {
			for (int index = 0; index < 8; index++) {
				if ((input & (1 << index)) != 0) {
					output |= 1 << this.andThen(VALUES[index]).ordinal();
				}
			}
			this.bulkCompose[input] = (byte)(output);
		}
		else {
			output &= 255;
		}
		return output;
	}
}