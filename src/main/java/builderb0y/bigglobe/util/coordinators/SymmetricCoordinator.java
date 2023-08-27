package builderb0y.bigglobe.util.coordinators;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public class SymmetricCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;
	public final byte symmetries;

	public SymmetricCoordinator(Coordinator delegate, int symmetries) {
		this.delegate = delegate;
		this.symmetries = (byte)(symmetries);
	}

	public static Coordinator create(Coordinator delegate, int symmetries) {
		if ((symmetries & 255) == Symmetry.IDENTITY.flag()) {
			return delegate;
		}
		else if ((symmetries & 255) == 0) {
			return Coordinator.warnDrop("No symmetries");
		}
		else {
			return new SymmetricCoordinator(delegate, symmetries);
		}
	}

	@Override
	public void genericPos(int x, int y, int z, CoordinatorRunnable callback) {
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				callback.run(this.delegate, symmetry.getX(x, z), y, symmetry.getZ(x, z));
			}
		}
	}

	@Override
	public <A> void genericPos(int x, int y, int z, A arg, CoordinatorConsumer<A> callback) {
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				callback.run(this.delegate, symmetry.getX(x, z), y, symmetry.getZ(x, z), arg);
			}
		}
	}

	@Override
	public <A, B> void genericPos(int x, int y, int z, A arg1, B arg2, CoordinatorBiConsumer<A, B> callback) {
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				callback.run(this.delegate, symmetry.getX(x, z), y, symmetry.getZ(x, z), arg1, arg2);
			}
		}
	}

	@Override
	public <A, B, C> void genericPos(int x, int y, int z, A arg1, B arg2, C arg3, CoordinatorTriConsumer<A, B, C> callback) {
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				callback.run(this.delegate, symmetry.getX(x, z), y, symmetry.getZ(x, z), arg1, arg2, arg3);
			}
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				this.delegate.setBlockState(symmetry.getX(x, z), y, symmetry.getZ(x, z), symmetry.apply(state));
			}
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		if (supplier == null) return;
		class SymmetricBlockStateSupplier implements CoordinateSupplier<BlockState> {

			public Symmetry currentSymmetry;

			@Override
			public BlockState get(BlockPos.Mutable pos) {
				BlockState state = supplier.get(pos);
				return state != null ? this.currentSymmetry.apply(state) : null;
			}
		}
		SymmetricBlockStateSupplier symmetricSupplier = new SymmetricBlockStateSupplier();
		int symmetries = this.symmetries;
		for (int index = 0; index < 8; index++) {
			if ((symmetries & (1 << index)) != 0) {
				Symmetry symmetry = Symmetry.VALUES[index];
				symmetricSupplier.currentSymmetry = symmetry;
				this.delegate.setBlockState(symmetry.getX(x, z), y, symmetry.getZ(x, z), symmetricSupplier);
			}
		}
	}

	@Override
	public Coordinator symmetric(Symmetry s1) {
		return create(this.delegate, s1.bulkAndThen(this.symmetries & 255));
	}

	@Override
	public Coordinator symmetric(Symmetry s1, Symmetry s2) {
		int s = this.symmetries & 255;
		return create(this.delegate, s1.bulkAndThen(s) | s2.bulkAndThen(s));
	}

	@Override
	public Coordinator symmetric(Symmetry s1, Symmetry s2, Symmetry s3, Symmetry s4) {
		int s = this.symmetries & 255;
		return create(this.delegate, s1.bulkAndThen(s) | s2.bulkAndThen(s) | s3.bulkAndThen(s) | s4.bulkAndThen(s));
	}

	@Override
	public Coordinator symmetric(Symmetry... symmetries) {
		int s = this.symmetries & 255;
		int flags = 0;
		for (Symmetry symmetry : symmetries) {
			flags |= symmetry.bulkAndThen(s);
		}
		return create(this.delegate, flags);
	}

	@Override
	public int hashCode() {
		return (this.symmetries & 255) * 31 + this.delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof SymmetricCoordinator that &&
			this.delegate.equals(that.delegate) &&
			this.symmetries == that.symmetries
		);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(this.delegate.toString()).append(" symmetric [");
		int symmetries = this.symmetries;
		if (symmetries != 0) {
			for (int index = 0; index < 8; index++) {
				if ((symmetries & (1 << index)) != 0) {
					builder.append(Symmetry.VALUES[index].name()).append(", ");
				}
			}
			builder.setLength(builder.length() - 2);
		}
		return builder.append(']').toString();
	}
}