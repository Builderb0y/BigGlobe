package builderb0y.bigglobe.util.coordinators;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.util.coordinators.AbstractLimitAreaCoordinator.InBox;
import builderb0y.bigglobe.util.coordinators.AbstractLimitAreaCoordinator.LazyInBox;
import builderb0y.bigglobe.util.coordinators.AbstractLimitAreaCoordinator.LimitArea;
import builderb0y.bigglobe.util.coordinators.AbstractPermuteCoordinator.*;
import builderb0y.bigglobe.util.coordinators.AbstractTranslateCoordinator.LazyTranslateCoordinator;
import builderb0y.bigglobe.util.coordinators.AbstractTranslateCoordinator.TranslateCoordinator;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.*;

/**
handle that allows pre-processing of coordinates
before using them to perform some action in a world.
*/
@SuppressWarnings({ "unused", "unchecked", "SameParameterValue" })
public interface Coordinator {

	public static final Logger LOGGER = LogManager.getLogger(BigGlobeMod.MODNAME + "/Coordinator");

	/**
	creates a Coordinator which delegates all calls to the provided world,
	without performing any pre-processing on the coordinates.
	the setBlockFlags are used as the flags in
	{@link StructureWorldAccess#setBlockState(BlockPos, BlockState, int)}
	when calling {@link #setBlockState(int, int, int, BlockState)} and similar methods.
	*/
	public static Coordinator forWorld(StructureWorldAccess world, int setBlockFlags) {
		return new BaseCoordinator(world, setBlockFlags);
	}

	/**
	returns a Coordinator which will relay all calls to every Coordinator in the array,
	in the same order that they occur in the array.
	*/
	public static Coordinator combine(Coordinator... coordinators) {
		for (Coordinator coordinator : coordinators) {
			if (coordinator instanceof CombinedCoordinator || coordinator instanceof DropCoordinator) {
				coordinators = (
					Arrays.stream(coordinators)
					.flatMap((Coordinator nested) ->
						nested instanceof CombinedCoordinator
						? Arrays.stream(((CombinedCoordinator)(nested)).delegates)
						: Stream.of(nested)
					)
					.filter((Coordinator nested) -> !(nested instanceof DropCoordinator))
					.distinct()
					.toArray(Coordinator[]::new)
				);
				break;
			}
		}
		return switch (coordinators.length) {
			case 0 -> warnDrop("coordinators array was empty, or contained only drops");
			case 1 -> coordinators[0];
			default -> new CombinedCoordinator(coordinators);
		};
	}

	/**
	some pre-processing operations could result in all coordinates being dropped.
	usually, this indicates a programming error.
	this method will print a warning to the log when such an operation happens.
	*/
	public static Coordinator warnDrop(String reason) {
		LOGGER.warn("A Coordinator option resulted in dropping all coordinates: ", new IllegalArgumentException(reason));
		return DropCoordinator.INSTANCE;
	}

	//////////////////////////////// actions ////////////////////////////////

	//invokes the action at the (possibly pre-processed) coordinates.
	public abstract void getCoordinates(int x, int y, int z, CoordinateConsumer action);

	public default void getCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateConsumer action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getCoordinates());
	}

	public default void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateConsumer action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getCoordinates());
	}

	public default void getCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, CoordinateConsumer... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getCoordinates(), actions);
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates, using the StructureWorldAccess associated
	with this Coordinator as the extra argument for the action.
	*/
	public abstract void getWorld(int x, int y, int z, CoordinateBiConsumer<StructureWorldAccess> action);

	public default void getWorldCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<StructureWorldAccess> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getWorld());
	}

	public default void getWorldLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<StructureWorldAccess> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getWorld());
	}

	public default void getWorldLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<StructureWorldAccess>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getWorld(), actions);
	}

	/**
	returns a Stream containing all the positions that the
	other action methods would attempt to perform an action on.
	*/
	public default Stream<BlockPos> streamCoordinates(int x, int y, int z) {
		Stream.Builder<BlockPos> builder = Stream.builder();
		this.getCoordinates(x, y, z, (BlockPos.Mutable pos) -> builder.accept(pos.toImmutable()));
		return builder.build();
	}

	public default Stream<BlockPos> streamCoordinatesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		Stream.Builder<BlockPos> builder = Stream.builder();
		this.getCoordinatesCuboid(minX, minY, minZ, maxX, maxY, maxZ, (BlockPos.Mutable pos) -> builder.accept(pos.toImmutable()));
		return builder.build();
	}

	public default Stream<BlockPos> streamCoordinatesLine(int x, int y, int z, int dx, int dy, int dz, int length) {
		Stream.Builder<BlockPos> builder = Stream.builder();
		this.getCoordinatesLine(x, y, z, dx, dy, dz, length, (BlockPos.Mutable pos) -> builder.accept(pos.toImmutable()));
		return builder.build();
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates, using the BlockState at those
	coordinates as the extra argument for the action.
	*/
	public abstract void getBlockState(int x, int y, int z, CoordinateBiConsumer<BlockState> action);

	public default void getBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<BlockState> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getBlockState());
	}

	public default void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<BlockState> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getBlockState());
	}

	public default void getBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<BlockState>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getBlockState(), actions);
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates, using the FluidState at those
	coordinates as the extra argument for the action.
	*/
	public abstract void getFluidState(int x, int y, int z, CoordinateBiConsumer<FluidState> action);

	public default void getFluidStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<FluidState> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getFluidState());
	}

	public default void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<FluidState> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getFluidState());
	}

	public default void getFluidStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<FluidState>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getFluidState(), actions);
	}

	/**
	if a BlockEntity exists at the (possibly pre-processed)
	coordinates, invokes the action at those coordinates
	using the BlockEntity as the extra argument for the action.
	*/
	public abstract void getBlockEntity(int x, int y, int z, CoordinateBiConsumer<BlockEntity> action);

	public default void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<BlockEntity> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getBlockEntity());
	}

	public default void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<BlockEntity> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getBlockEntity());
	}

	public default void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<BlockEntity>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getBlockEntity(), actions);
	}

	/**
	if a BlockEntity of the requested type exists at the (possibly
	pre-processed) coordinates, invokes the action at those coordinates
	using the {@link BlockEntity} as the extra argument for the action.
	the generic type B is not bounded to extend BlockEntity to
	allow it to be an interface type. for example, {@link Inventory}.
	*/
	public abstract <B> void getBlockEntity(int x, int y, int z, Class<B> blockEntityType, CoordinateBiConsumer<B> action);

	public default <B> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<B> blockEntityType, CoordinateBiConsumer<B> action) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.getBlockEntity(x, y, z, blockEntityType, action);
				}
			}
		}
	}

	public default <B> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (length <= 0) return;
		this.getBlockEntity(x, y, z, blockEntityType, action);
		for (int index = 0; index < length; index++) {
			this.getBlockEntity(x += dx, y += dy, z += dz, blockEntityType, action);
		}
	}

	public default <B> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, Class<B> blockEntityType, CoordinateBiConsumer<B>... actions) {
		int length = actions.length;
		if (length == 0) return;
		if (actions[0] != null) {
			this.getBlockEntity(x, y, z, blockEntityType, actions[0]);
		}
		for (int index = 1; index < length; index++) {
			x += dx; y += dy; z += dz;
			if (actions[index] != null) {
				this.getBlockEntity(x, y, z, blockEntityType, actions[index]);
			}
		}
	}

	/**
	if a BlockEntity of the requested type exists at the (possibly
	pre-processed) coordinates, invokes the action at those coordinates
	using the BlockEntity as the extra argument for the action.
	*/
	public abstract <B extends BlockEntity> void getBlockEntity(int x, int y, int z, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action);

	public default <B extends BlockEntity> void getBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.getBlockEntity(x, y, z, blockEntityType, action);
				}
			}
		}
	}

	public default <B extends BlockEntity> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (length <= 0) return;
		this.getBlockEntity(x, y, z, blockEntityType, action);
		for (int index = 0; index < length; index++) {
			this.getBlockEntity(x += dx, y += dy, z += dz, blockEntityType, action);
		}
	}

	public default <B extends BlockEntity> void getBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B>... actions) {
		int length = actions.length;
		if (length == 0) return;
		if (actions[0] != null) {
			this.getBlockEntity(x, y, z, blockEntityType, actions[0]);
		}
		for (int index = 1; index < length; index++) {
			x += dx; y += dy; z += dz;
			if (actions[index] != null) {
				this.getBlockEntity(x += dx, y += dy, z += dz, blockEntityType, actions[index]);
			}
		}
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates, using the Biome at those
	coordinates as the extra argument for the action.
	note: the biome is fetched via a {@link WorldColumn},
	not through {@link StructureWorldAccess#getBiome(BlockPos)}.
	this can sometimes provide more accurate biome sampling,
	due to the fact that chunks only store on biome for every 4x4x4 volume,
	but columns can compute the biome for every block.
	*/
	public abstract void getBiome(int x, int y, int z, CoordinateBiConsumer<RegistryEntry<Biome>> action);

	public default void getBiomeCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getBiome());
	}

	public default void getBiomeLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<RegistryEntry<Biome>> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getBiome());
	}

	public default void getBiomeLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<RegistryEntry<Biome>>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getBiome(), actions);
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates, using the Chunk at those
	coordinates as the extra argument for the action.
	*/
	public abstract void getChunk(int x, int y, int z, CoordinateBiConsumer<Chunk> action);

	public default void getChunkCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateBiConsumer<Chunk> action) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, action, CuboidBiCallback.getChunk());
	}

	public default void getChunkLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateBiConsumer<Chunk> action) {
		this.genericLine(x, y, z, dx, dy, dz, length, action, LineBiCallback.getChunk());
	}

	public default void getChunkLine(int x, int y, int z, int dx, int dy, int dz, CoordinateBiConsumer<Chunk>... actions) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.getChunk(), actions);
	}

	/**
	sets the BlockState at the (possibly pre-processed)
	coordinates to the provided BlockState.
	if the provided BlockState is null, this method does nothing.
	*/
	public abstract void setBlockState(int x, int y, int z, BlockState state);

	public default void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, state, CuboidBiCallback.setBlockState());
	}

	public default void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state) {
		this.genericLine(x, y, z, dx, dy, dz, length, state, LineBiCallback.setBlockState());
	}

	public default void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, BlockState... states) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.setBlockState(), states);
	}

	/**
	sets the BlockState at the (possibly pre-processed)
	coordinates to the provided BlockState.
	then, if a BlockEntity which is an instance of B
	was created as a result of setting the BlockState,
	the provided action will be invoked on it.

	this method is a shortcut to calling
	setBlockState() followed by getBlockEntity().
	however, if the provided BlockState is null,
	this method does nothing. this means that if a suitable
	BlockEntity existed at the (possibly pre-processed)
	coordinates BEFORE this method was called,
	the action will NOT be invoked on it.
	*/
	public abstract <B> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action);

	public default <B> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
				}
			}
		}
	}

	public default <B> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, Class<B> blockEntityClass, CoordinateBiConsumer<B> action) {
		if (length <= 0) return;
		this.setBlockStateAndBlockEntity(x, y, z, state, blockEntityClass, action);
		for (int index = 1; index < length; index++) {
			this.setBlockStateAndBlockEntity(x += dx, y += dy, z += dz, state, blockEntityClass, action);
		}
	}

	/**(
	sets the BlockState at the (possibly pre-processed)
	coordinates to the provided BlockState.
	then, if a BlockEntity which is an instance of B
	was created as a result of setting the BlockState,
	the provided action will be invoked on it.

	this method is a shortcut to calling
	setBlockState() followed by getBlockEntity().
	however, if the provided BlockState is null,
	this method does nothing. this means that if a suitable
	BlockEntity existed at the (possibly pre-processed)
	coordinates BEFORE this method was called,
	the action will NOT be invoked on it.
	*/
	public abstract <B extends BlockEntity> void setBlockStateAndBlockEntity(int x, int y, int z, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action);

	public default <B extends BlockEntity> void setBlockStateAndBlockEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null) return;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
				}
			}
		}
	}

	public default <B extends BlockEntity> void setBlockStateAndBlockEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, BlockState state, BlockEntityType<B> blockEntityType, CoordinateBiConsumer<B> action) {
		if (state == null || length <= 0) return;
		this.setBlockStateAndBlockEntity(x, y, z, state, blockEntityType, action);
		for (int index = 1; index < length; index++) {
			this.setBlockStateAndBlockEntity(x += dx, y += dy, z += dz, state, blockEntityType, action);
		}
	}

	/**
	sets the BlockState at the (possibly pre-processed)
	coordinates to the BlockState provided by the supplier,
	if the supplier supplies null, this method does nothing.
	note: if coordinate pre-processing results in coordinates
	being duplicated, the supplier will be invoked on all of them.
	in other words, this method will NOT abort on
	the first null returned by the supplier.
	*/
	public abstract void setBlockState(int x, int y, int z, CoordinateSupplier<BlockState> supplier);

	public default void setBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateSupplier<BlockState> supplier) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, supplier, CuboidBiCallback.setBlockState_supplier());
	}

	public default void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateSupplier<BlockState> supplier) {
		this.genericLine(x, y, z, dx, dy, dz, length, supplier, LineBiCallback.setBlockState_supplier());
	}

	public default void setBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateSupplier<BlockState>... suppliers) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.setBlockState_supplier(), suppliers);
	}

	/**
	like the above methods, but invokes the supplier on the provided
	coodinates immediately without pre-processing them first.
	the return value of the supplier will be applied to the pre-processed coordinates.
	*/
	public abstract void setBlockStateRelative(int x, int y, int z, CoordinateSupplier<BlockState> supplier);

	public default void setBlockStateRelativeCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateSupplier<BlockState> supplier) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, supplier, CuboidBiCallback.setBlockStateRelative());
	}

	public default void setBlockStateRelativeLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateSupplier<BlockState> supplier) {
		this.genericLine(x, y, z, dx, dy, dz, length, supplier, LineBiCallback.setBlockStateRelative());
	}

	public default void setBlockStateRelativeLine(int x, int y, int z, int dx, int dy, int dz, CoordinateSupplier<BlockState>... suppliers) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.setBlockStateRelative(), suppliers);
	}

	/**
	first, invokes the mapper at the (possibly pre-processed)
	coordinates, using the BlockState at those
	coordinates as the extra argument for the mapper.
	next, sets the BlockState at those coordinates
	to the BlockState returned by the mapper.
	if the mapper returns null (or the same BlockState
	that was provided to it), this method does nothing.
	note: if coordinate pre-processing results in coordinates
	being duplicated, the mapper will be invoked on all of them.
	in other words, this method will NOT abort on
	the first null returned by the mapper.
	*/
	public abstract void modifyBlockState(int x, int y, int z, CoordinateUnaryOperator<BlockState> mapper);

	public default void modifyBlockStateCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateUnaryOperator<BlockState> mapper) {
		this.genericCuboid(minX, minY, minZ, maxX, maxY, maxZ, mapper, CuboidBiCallback.modifyBlockState());
	}

	public default void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateUnaryOperator<BlockState> mapper) {
		this.genericLine(x, y, z, dx, dy, dz, length, mapper, LineBiCallback.modifyBlockState());
	}

	public default void modifyBlockStateLine(int x, int y, int z, int dx, int dy, int dz, CoordinateUnaryOperator<BlockState>... mappers) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.modifyBlockState(), mappers);
	}

	/**
	first, invokes the boxSupplier at the (possibly pre-processed) coordinates.
	next, gets the list of entities of the specified type in that box.
	lastly, invokes the entityAction at those coordinates using the
	list of entities as the extra argument for the entityAction.

	I don't like the parameters of this method.
	I think it would make much more sense to provide an
	AABB instead of 3 ints, drop the boxSupplier parameter,
	and a new callback interface which also takes AABBs.
	but I would need to do a decent amount of re-writing
	(mostly in {@link AbstractPermuteCoordinator}) to handle this,
	and that's a lot of work for just one method.
	so for now at least, these parameters will stay.
	*/
	public abstract <E extends Entity> void getEntities(int x, int y, int z, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction);

	public default <E extends Entity> void getEntitiesCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.getEntities(x, y, z, entityType, boxSupplier, entityAction);
				}
			}
		}
	}

	public default <E extends Entity> void getEntitiesLine(int x, int y, int z, int dx, int dy, int dz, int length, Class<E> entityType, CoordinateSupplier<Box> boxSupplier, CoordinateBiConsumer<List<E>> entityAction) {
		if (length <= 0) return;
		this.getEntities(x, y, z, entityType, boxSupplier, entityAction);
		for (int index = 0; index < length; index++) {
			this.getEntities(x += dx, y += dy, z += dz, entityType, boxSupplier, entityAction);
		}
	}

	//no getEntitiesLine() method which takes arrays because
	//there are 3 different parameters which the user could
	//potentially want to be different for each position,
	//and that would require 8 different methods.
	//genericLine() can be used instead if absolutely necessary.

	/**
	invokes the supplier at the (possibly pre-processed)
	coordinates, and adds the returned Entity to the world.
	if the supplier returns null, this method does nothing.
	note: if coordinate pre-processing results in coordinates
	being duplicated, the supplier will be invoked on all of them.
	in other words, this method will NOT abort on
	the first null returned by the supplier.
	*/
	public abstract void addEntity(int x, int y, int z, CoordinateFunction<ServerWorld, Entity> supplier);

	public default void addEntityCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CoordinateFunction<ServerWorld, Entity> supplier) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					this.addEntity(x, y, z, supplier);
				}
			}
		}
	}

	public default void addEntityLine(int x, int y, int z, int dx, int dy, int dz, int length, CoordinateFunction<ServerWorld, Entity> supplier) {
		this.genericLine(x, y, z, dx, dy, dz, length, supplier, LineBiCallback.addEntity());
	}

	public default void addEntityLine(int x, int y, int z, int dx, int dy, int dz, CoordinateFunction<ServerWorld, Entity>... suppliers) {
		this.genericLine(x, y, z, dx, dy, dz, LineBiCallback.addEntity(), suppliers);
	}

	/**
	invokes the action at the coordinates
	several times in a cuboid region.
	*/
	public default void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CuboidCallback action) {
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					action.run(this, x, y, z);
				}
			}
		}
	}

	/**
	invokes the action at the coordinates
	several times in a cuboid region
	using the extra argument for the action.
	*/
	public default <T> void genericCuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, T arg, CuboidBiCallback<T> action) {
		if (arg == null) return;
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					action.run(this, x, y, z, arg);
				}
			}
		}
	}

	/**
	invokes the action at the coordinates
	several times in a straight line.
	*/
	public default void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, LineCallback action) {
		if (length <= 0) return;
		action.run(this, x, y, z, 0);
		for (int index = 1; index < length; index++) {
			x += dx; y += dy; z += dz;
			action.run(this, x, y, z, index);
		}
	}

	/**
	invokes the action at the (possibly pre-processed)
	coordinates several times in a straight line,
	using the elements in the provided array
	as the extra argument for the action.
	the array parameter is last so that it can use varargs.
	*/
	public default <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, LineBiCallback<T> action, T... args) {
		int length = args.length;
		if (length == 0) return;
		if (args[0] != null) action.run(this, x, y, z, 0, args[0]);
		for (int index = 1; index < length; index++) {
			x += dx; y += dy; z += dz;
			if (args[index] != null) action.run(this, x, y, z, index, args[index]);
		}
	}

	/**
	invokes the action at the coordinates
	several times in a straight line
	using the extra argument for the action.
	the argument parameter is NOT last because:
	A: it does not need to be varargs, and
	B: this will add a bit more distinction from the previous method.
	*/
	public default <T> void genericLine(int x, int y, int z, int dx, int dy, int dz, int length, T arg, LineBiCallback<T> action) {
		if (arg == null || length <= 0) return;
		action.run(this, x, y, z, 0, arg);
		for (int index = 1; index < length; index++) {
			action.run(this, x += dx, y += dy, z += dz, index, arg);
		}
	}

	//////////////////////////////// coordinate manipulation ////////////////////////////////

	/** translates coordinates by the specified offset before using them. */
	public default Coordinator translate(int offsetX, int offsetY, int offsetZ) {
		if (offsetX == 0 && offsetY == 0 && offsetZ == 0) return this;
		else return new TranslateCoordinator(this, offsetX, offsetY, offsetZ);
	}

	/**
	translates coordinates by the specified offset before using them.
	if the offset is a mutable position, then lazy can be set to true
	to allow the offset to be changed after this method returns.
	*/
	public default Coordinator translate(Vec3i offset, boolean lazy) {
		if (lazy) {
			return new LazyTranslateCoordinator(this, offset);
		}
		else {
			return this.translate(offset.getX(), offset.getY(), offset.getZ());
		}
	}

	/**
	translates coordinates by multiple different offsets before using them.
	example usage:
	multiTranslate(
		1, 0, 0,
		0, 1, 0,
		0, 0, 1
	)
	is equivalent to:
	combine(
		translate(1, 0, 0),
		translate(0, 1, 0),
		translate(0, 0, 1)
	)
	if the offsets array's length is not a multiple of 3, throws an IllegalArgumentException.
	if the offsets array contains duplicate triplets, only one will be used.
	*/
	public default Coordinator multiTranslate(int... offsets) {
		if (offsets.length == 0) return warnDrop("no offsets provided");
		if (offsets.length == 3) return this.translate(offsets[0], offsets[1], offsets[2]);
		int count = offsets.length / 3;
		if (offsets.length != count * 3) throw new IllegalArgumentException("offsets length must be multiple of 3");
		return combine(
			IntStream.range(0, count)
			.map((int index) -> index + (index << 1))
			.mapToObj((int base) -> this.translate(offsets[base], offsets[base + 1], offsets[base + 2]))
			.distinct()
			.toArray(Coordinator[]::new)
		);
	}

	/**
	translates coordinates by multiple different offsets before using them.
	example usage:
	multiTranslate(
		new Vec3i(1, 0, 0),
		new Vec3i(0, 1, 0),
		new Vec3i(0, 0, 1)
	)
	is equivalent to:
	combine(
		translate(1, 0, 0),
		translate(0, 1, 0),
		translate(0, 0, 1)
	)
	if the offsets array contains duplicate vectors, only one will be used.
	for technical reasons, this method does not have a lazy parameter.
	this is because it would be easy to detect when an element in the array changes,
	but it would not be easy to detect when an element in the array is directly replaced.
	in other words, offsets[0].setPos(1, 2, 3) is easy to detect,
	but offsets[0] = new BlockPos(1, 2, 3) is hard to detect.
	*/
	public default Coordinator multiTranslate(Vec3i... offsets) {
		if (offsets.length == 0) return warnDrop("no offsets provided");
		if (offsets.length == 1) return this.translate(offsets[0], false);
		return combine(
			Arrays.stream(offsets)
			.map((Vec3i offset) -> this.translate(offset, false))
			.distinct()
			.toArray(Coordinator[]::new)
		);
	}

	/**
	rotates all coordinates about the origin by the provided amount before using them.
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support rotation via
	{@link BlockState#rotate(BlockRotation)} will be rotated accordingly.
	*/
	public default Coordinator rotate1x(BlockRotation rotation) {
		return switch (rotation) {
			case NONE                -> this;
			case CLOCKWISE_90        -> new Rotate1x90(this);
			case CLOCKWISE_180       -> new Rotate1x180(this);
			case COUNTERCLOCKWISE_90 -> new Rotate1x270(this);
		};
	}

	/**
	duplicates all coordinates 4 times,
	rotated around the Y axis in units of 90 degrees.
	to rotate around a different point,
	use translate(centerOfRotation).rotate4x90().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support rotation via
	{@link BlockState#rotate(BlockRotation)} will be rotated accordingly.
	*/
	public default Coordinator rotate4x90() {
		return new Rotate4x90(this);
	}

	/**
	duplicates all coordinates twice,
	rotated around the Y axis in units of 180 degrees.
	to rotate around a different point,
	use translate(centerOfRotation).rotate2x180().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support rotation via
	{@link BlockState#rotate(BlockRotation)} will be rotated accordingly.
	*/
	public default Coordinator rotate2x180() {
		return new Rotate2x180(this);
	}

	/**
	flips all coordinates along the X axis, centered at X = 0.
	to flip around a different center point,
	use translate(centerOfFlip).flip1X().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support flipping via
	{@link BlockState#mirror(BlockMirror)} will be flipped accordingly.
	*/
	public default Coordinator flip1X() {
		return new Flip1X(this);
	}

	/**
	flips all coordinates along the Z axis, centered at Z = 0.
	to flip around a different center point,
	use translate(centerOfFlip).flip1Z().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support flipping via
	{@link BlockState#mirror(BlockMirror)} will be flipped accordingly.
	*/
	public default Coordinator flip1Z() {
		return new Flip1Z(this);
	}

	/**
	duplicates all coordinates twice,
	where one copy is left as-is, and the other is
	flipped along the X axis, centered at X = 0.
	to flip around a different center point,
	use translate(centerOfFlip).flip2X().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support flipping via
	{@link BlockState#mirror(BlockMirror)} will be flipped accordingly.
	*/
	public default Coordinator flip2X() {
		return new Flip2X(this);
	}

	/**
	duplicates all coordinates twice,
	where one copy is left as-is, and the other is
	flipped along the Z axis, centered at Z = 0.
	to flip around a different center point,
	use translate(centerOfFlip).flip2Z().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support flipping via
	{@link BlockState#mirror(BlockMirror)} will be flipped accordingly.
	*/
	public default Coordinator flip2Z() {
		return new Flip2Z(this);
	}

	/**
	duplicates all coordinates 4 times,
	flipped along the X and X axes, centered at X = 0 and Z = 0.
	to flip around a different center point,
	use translate(centerOfFlip).flip4XZ().
	when calling {@link #setBlockState(int, int, int, BlockState)}
	or similar methods, any blocks which support flipping via
	{@link BlockState#mirror(BlockMirror)} will be flipped accordingly.
	*/
	public default Coordinator flip4XZ() {
		return new Flip4XZ(this);
	}

	/**
	duplicates all coordinates count times in a straight line.
	the name comes from the worldedit command "//stack".
	to duplicate coordinates in a 2D grid pattern,
	use stack(x1, y1, z1, count1).stack(x2, y2, z2, count2) for
	the unit vectors (x1, y1, z1) and (x2, y2, z2).
	the same approach can be used to duplicate blocks in a
	3D grid pattern, using 3 chained invocations of stack().
	*/
	public default Coordinator stack(int dx, int dy, int dz, int count) {
		if (count <= 0) return warnDrop("Count was less than or equal to 0");
		if (count == 1 || (dx == 0 && dy == 0 && dz == 0)) return this;
		return new StackCoordinator(this, dx, dy, dz, count);
	}

	/**
	restricts coordinates based on the provided predicate.
	no actions will be performed on coordinates which the predicate rejects.
	*/
	public default Coordinator limitArea(CoordinatePredicate predicate) {
		return new LimitArea(this, predicate);
	}

	/**
	restricts coordinates based on the provided box.
	no actions will be performed on coordinates which are outside this box.
	the bounds of the box are INCLUSIVE!
	*/
	public default Coordinator inBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if (maxX >= minX && maxZ >= minZ && maxY >= minY) {
			return new InBox(this, minX, minY, minZ, maxX, maxY, maxZ);
		}
		else {
			return warnDrop("Box was invalid; at least one min value was greater than the corresponding max value");
		}
	}

	/**
	restricts coordinates based on the provided box.
	no actions will be performed on coordinates which are outside this box.
	if lazy is set to true, then the box can be modified after this method returns,
	and the returned Coordinator will respect these modifications.
	the bounds of the box are INCLUSIVE!
	*/
	public default Coordinator inBox(BlockBox box, boolean lazy) {
		if (lazy) {
			return new LazyInBox(this, box);
		}
		else {
			return this.inBox(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
		}
	}
}