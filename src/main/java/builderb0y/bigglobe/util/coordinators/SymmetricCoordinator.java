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
		return new SymmetricCoordinator(this.delegate, s1.bulkCompose(this.symmetries & 255));
	}

	@Override
	public Coordinator symmetric(Symmetry s1, Symmetry s2) {
		int s = this.symmetries & 255;
		return new SymmetricCoordinator(this.delegate, s1.bulkCompose(s) | s2.bulkCompose(s));
	}

	@Override
	public Coordinator symmetric(Symmetry s1, Symmetry s2, Symmetry s3, Symmetry s4) {
		int s = this.symmetries & 255;
		return new SymmetricCoordinator(this.delegate, s1.bulkCompose(s) | s2.bulkCompose(s) | s3.bulkCompose(s) | s4.bulkCompose(s));
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