package builderb0y.bigglobe.util.coordinators;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

public abstract class AbstractPermuteCoordinator extends ScratchPosCoordinator {

	public final Coordinator delegate;

	public AbstractPermuteCoordinator(Coordinator delegate) {
		this.delegate = delegate;
	}

	public static enum Permutation {
		IDENTITY {
			@Override public int x(int x, int z) { return x; }
			@Override public int z(int x, int z) { return z; }
			@Override public BlockState state(BlockState state) { return state; }
			@Override public float entityYaw(float oldYaw) { return oldYaw; }
		},
		ROTATE_90 {
			@Override public int x(int x, int z) { return -z; }
			@Override public int z(int x, int z) { return x; }
			@Override public BlockState state(BlockState state) { return state.rotate(BlockRotation.CLOCKWISE_90); }
			@Override public float entityYaw(float oldYaw) { return oldYaw + 90.0F; }
		},
		ROTATE_180 {
			@Override public int x(int x, int z) { return -x; }
			@Override public int z(int x, int z) { return -z; }
			@Override public BlockState state(BlockState state) { return state.rotate(BlockRotation.CLOCKWISE_180); }
			@Override public float entityYaw(float oldYaw) { return oldYaw + 180.0F; }
		},
		ROTATE_270 {
			@Override public int x(int x, int z) { return z; }
			@Override public int z(int x, int z) { return -x; }
			@Override public BlockState state(BlockState state) { return state.rotate(BlockRotation.COUNTERCLOCKWISE_90); }
			@Override public float entityYaw(float oldYaw) { return oldYaw - 90.0F; }
		},
		FLIP_X {
			@Override public int x(int x, int z) { return -x; }
			@Override public int z(int x, int z) { return z; }
			@Override public BlockState state(BlockState state) { return state.mirror(BlockMirror.FRONT_BACK); }
			@Override public float entityYaw(float oldYaw) { return -oldYaw; }
		},
		FLIP_Z {
			@Override public int x(int x, int z) { return x; }
			@Override public int z(int x, int z) { return -z; }
			@Override public BlockState state(BlockState state) { return state.mirror(BlockMirror.LEFT_RIGHT); }
			@Override public float entityYaw(float oldYaw) { return 180.0F - oldYaw; }
		},
		FLIP_XZ {
			@Override public int x(int x, int z) { return -x; }
			@Override public int z(int x, int z) { return -z; }
			@Override public BlockState state(BlockState state) { return state.mirror(BlockMirror.FRONT_BACK).mirror(BlockMirror.LEFT_RIGHT); }
			@Override public float entityYaw(float oldYaw) { return oldYaw + 180.0F; }
		};

		public abstract int x(int x, int z);
		public abstract int z(int x, int z);
		public abstract BlockState state(BlockState state);
		public abstract float entityYaw(float oldYaw);
	}

	public static class PermutedBlockStateSupplier implements CoordinateSupplier<BlockState> {

		public final CoordinateSupplier<BlockState> delegate;
		public Permutation permutation;

		public PermutedBlockStateSupplier(CoordinateSupplier<BlockState> delegate) {
			this.delegate = delegate;
		}

		@Override
		public BlockState get(BlockPos.Mutable pos) {
			BlockState state = this.delegate.get(pos);
			return state != null ? this.permutation.state(state) : null;
		}
	}

	public static class PermutedEntityFunction implements CoordinateFunction<ServerWorld, Entity> {

		public final CoordinateFunction<ServerWorld, Entity> delegate;
		public Permutation permutation;

		public PermutedEntityFunction(CoordinateFunction<ServerWorld, Entity> delegate) {
			this.delegate = delegate;
		}

		@Override
		public Entity apply(BlockPos.Mutable pos, ServerWorld world) {
			Entity entity = this.delegate.apply(pos, world);
			if (entity != null) {
				entity.setYaw(BigGlobeMath.modulus_BP(this.permutation.entityYaw(entity.getYaw()) + 180.0F, 360.0F) - 180.0F);
				entity.prevYaw = BigGlobeMath.modulus_BP(this.permutation.entityYaw(entity.prevYaw) + 180.0F, 360.0F) - 180.0F;
			}
			return entity;
		}
	}

	public abstract Permutation[] permutations();

	@Override
	public @Nullable BlockPos getCoordinate(int x, int y, int z) {
		Permutation[] permutations = this.permutations();
		if (permutations.length == 1) {
			Permutation permutation = permutations[0];
			return this.delegate.getCoordinate(permutation.x(x, z), y, permutation.z(x, z));
		}
		else {
			return null;
		}
	}

	@Override
	public StructureWorldAccess getWorld() {
		return this.delegate.getWorld();
	}

	@Override
	public void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getWorld(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void getCoordinates(int x, int y, int z, CoordinateConsumer action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getCoordinates(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getBlockState(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getFluidState(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getBlockEntity(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public <B> void getBlockEntity(int x, int y, int z, Class<B> tileEntityType, CoordinateBiConsumer<B> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getBlockEntity(permutation.x(x, z), y, permutation.z(x, z), tileEntityType, action);
		}
	}

	@Override
	public <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> tileEntityType, CoordinateBiConsumer<B> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getBlockEntity(permutation.x(x, z), y, permutation.z(x, z), tileEntityType, action);
		}
	}

	@Override
	public void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getBiome(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getChunk(permutation.x(x, z), y, permutation.z(x, z), action);
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, BlockState state) {
		if (state == null) return;
		for (Permutation permutation : this.permutations()) {
			this.delegate.setBlockState(permutation.x(x, z), y, permutation.z(x, z), permutation.state(state));
		}
	}

	@Override
	public void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier) {
		PermutedBlockStateSupplier permutedSupplier = new PermutedBlockStateSupplier(supplier);
		for (Permutation permutation : this.permutations()) {
			permutedSupplier.permutation = permutation;
			this.delegate.setBlockState(permutation.x(x, z), y, permutation.z(x, z), permutedSupplier);
		}
	}

	@Override
	public <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		for (Permutation permutation : this.permutations()) {
			this.delegate.setBlockStateAndBlockEntity(permutation.x(x, z), y, permutation.z(x, z), permutation.state(state), blockEntityClass, action);
		}
	}

	@Override
	public <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		for (Permutation permutation : this.permutations()) {
			this.delegate.setBlockStateAndBlockEntity(permutation.x(x, z), y, permutation.z(x, z), permutation.state(state), blockEntityType, action);
		}
	}

	@Override
	public void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.modifyBlockState(permutation.x(x, z), y, permutation.z(x, z), mapper);
		}
	}

	@Override
	public <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		for (Permutation permutation : this.permutations()) {
			this.delegate.getEntities(permutation.x(x, z), y, permutation.z(x, z), entityType, boxSupplier, entityAction);
		}
	}

	@Override
	public void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier) {
		PermutedEntityFunction permutedSupplier = new PermutedEntityFunction(supplier);
		for (Permutation permutation : this.permutations()) {
			permutedSupplier.permutation = permutation;
			this.delegate.addEntity(permutation.x(x, z), y, permutation.z(x, z), permutedSupplier);
		}
	}

	@Override
	public int hashCode() {
		return this.delegate.hashCode() ^ Arrays.hashCode(this.permutations());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AbstractPermuteCoordinator)) return false;
		AbstractPermuteCoordinator that = (AbstractPermuteCoordinator)(obj);
		return this.delegate.equals(that.delegate) && Arrays.equals(this.permutations(), that.permutations());
	}

	@Override
	public String toString() {
		return this.delegate + " permuted " + Arrays.toString(this.permutations());
	}

	public static class Rotate1x90 extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.ROTATE_90 };

		public Rotate1x90(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator rotate1x(BlockRotation rotation) {
			return this.delegate.rotate1x(rotation.rotate(BlockRotation.CLOCKWISE_90));
		}

		@Override
		public Coordinator rotate4x90() {
			return this.delegate.rotate4x90();
		}
	}

	public static class Rotate1x180 extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.ROTATE_180 };

		public Rotate1x180(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator rotate1x(BlockRotation rotation) {
			return this.delegate.rotate1x(rotation.rotate(BlockRotation.CLOCKWISE_180));
		}

		@Override
		public Coordinator rotate4x90() {
			return this.delegate.rotate4x90();
		}
	}

	public static class Rotate1x270 extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.ROTATE_270 };

		public Rotate1x270(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator rotate1x(BlockRotation rotation) {
			return this.delegate.rotate1x(rotation.rotate(BlockRotation.COUNTERCLOCKWISE_90));
		}

		@Override
		public Coordinator rotate4x90() {
			return this.delegate.rotate4x90();
		}
	}

	public static class Rotate4x90 extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.IDENTITY, Permutation.ROTATE_90, Permutation.ROTATE_180, Permutation.ROTATE_270 };

		public Rotate4x90(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator rotate1x(BlockRotation rotation) {
			return this;
		}

		@Override
		public Coordinator rotate4x90() {
			return this;
		}

		@Override
		public Coordinator rotate2x180() {
			return this;
		}
	}

	public static class Rotate2x180 extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.IDENTITY, Permutation.ROTATE_180 };

		public Rotate2x180(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator rotate2x180() {
			return this;
		}

		@Override
		public Coordinator rotate4x90() {
			return this.delegate.rotate4x90();
		}
	}

	public static class Flip1X extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.FLIP_X };

		public Flip1X(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Coordinator flip1X() {
			return this.delegate;
		}

		@Override
		public Coordinator flip2X() {
			return this.delegate.flip2X();
		}

		@Override
		public Coordinator flip4XZ() {
			return this.delegate.flip4XZ();
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}
	}

	public static class Flip1Z extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.FLIP_Z };

		public Flip1Z(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Coordinator flip1Z() {
			return this.delegate;
		}

		@Override
		public Coordinator flip2Z() {
			return this.delegate.flip2Z();
		}

		@Override
		public Coordinator flip4XZ() {
			return this.delegate.flip4XZ();
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}
	}

	public static class Flip2X extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.IDENTITY, Permutation.FLIP_X };

		public Flip2X(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator flip2X() {
			return this;
		}

		@Override
		public Coordinator flip2Z() {
			return new Flip4XZ(this.delegate);
		}

		@Override
		public Coordinator flip4XZ() {
			return new Flip4XZ(this.delegate);
		}
	}

	public static class Flip2Z extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.IDENTITY, Permutation.FLIP_Z };

		public Flip2Z(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator flip2X() {
			return new Flip4XZ(this.delegate);
		}

		@Override
		public Coordinator flip2Z() {
			return this;
		}

		@Override
		public Coordinator flip4XZ() {
			return new Flip4XZ(this.delegate);
		}
	}

	public static class Flip4XZ extends AbstractPermuteCoordinator {

		public static final Permutation[] PERMUTATIONS = { Permutation.IDENTITY, Permutation.FLIP_X, Permutation.FLIP_XZ, Permutation.FLIP_Z };

		public Flip4XZ(Coordinator delegate) {
			super(delegate);
		}

		@Override
		public Permutation[] permutations() {
			return PERMUTATIONS;
		}

		@Override
		public Coordinator flip2X() {
			return this;
		}

		@Override
		public Coordinator flip2Z() {
			return this;
		}

		@Override
		public Coordinator flip4XZ() {
			return this;
		}
	}
}