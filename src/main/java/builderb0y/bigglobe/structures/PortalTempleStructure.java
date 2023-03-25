package builderb0y.bigglobe.structures;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.*;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.math.*;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.WorldUtil;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateSupplier;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class PortalTempleStructure extends BigGlobeStructure {

	public static final Codec<PortalTempleStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(PortalTempleStructure.class);

	public final RandomSource cracked_chance;

	public PortalTempleStructure(Config config, RandomSource cracked_chance) {
		super(config);
		this.cracked_chance = cracked_chance;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		Permuter permuter = Permuter.from(context.random());
		int x = context.chunkPos().getStartX() | (permuter.nextInt() & 15);
		int z = context.chunkPos().getStartZ() | (permuter.nextInt() & 15);
		VerticalBlockSample sample = context.chunkGenerator().getColumnSample(x, z, context.world(), context.noiseConfig());
		int minY = context.chunkGenerator().getMinimumY();
		int maxY = minY + context.chunkGenerator().getWorldHeight();
		for (int startAttempt = 0; startAttempt < 16; startAttempt++) {
			int y = permuter.nextInt(minY, maxY);
			if (sample.getState(y).isAir()) {
				do y--;
				while (y >= minY && sample.getState(y).isAir());
				if (sample.getState(y).isOpaqueFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
					int y_ = y;
					return Optional.of(
						new StructurePosition(
							new BlockPos(x, y, z),
							collector -> {
								List<StructurePiece> pieces = new ArrayList<>(8);
								double crackedChance = this.cracked_chance.get(permuter);
								pieces.add(new MainBuildingPiece(BigGlobeStructures.PORTAL_TEMPLE_MAIN_BUILDING, x, y_,      z, crackedChance, permuter));
								pieces.add(new       PortalPiece(BigGlobeStructures.PORTAL_TEMPLE_PORTAL,        x, y_ + 10, z,                permuter));
								for (int failure = 0; failure < 4;) {
									DecorationPiece piece = switch (permuter.nextInt() & 3) {
										case 0 -> new    WellPiece(BigGlobeStructures.PORTAL_TEMPLE_WELL,    x, y_, z, permuter);
										case 1 -> new    FarmPiece(BigGlobeStructures.PORTAL_TEMPLE_FARM,    x, y_, z, permuter);
										case 2 -> new   TablePiece(BigGlobeStructures.PORTAL_TEMPLE_TABLE,   x, y_, z, permuter);
										case 3 -> new FurnacePiece(BigGlobeStructures.PORTAL_TEMPLE_FURNACE, x, y_, z, permuter);
										default -> throw new AssertionError();
									};
									//allow up to 4 failures.
									if (!addDecoration(pieces, piece)) failure++;
								}
								addDecoration(pieces, new SpawnerPiece(BigGlobeStructures.PORTAL_TEMPLE_SPAWNER, x, y_, z, permuter));
								for (StructurePiece piece : pieces) collector.addPiece(piece);
							}
						)
					);
				}
			}
		}
		return Optional.empty();
	}

	public static boolean addDecoration(List<StructurePiece> pieces, DecorationPiece piece) {
		BlockBox box = piece.getBoundingBox().expand(1);
		for (int index = 2, size = pieces.size(); index < size; index++) {
			if (box.intersects(pieces.get(index).getBoundingBox())) {
				return false;
			}
		}
		pieces.add(piece);
		return true;
	}

		@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.PORTAL_TEMPLE;
	}

	public static CoordinateSupplier<BlockState> netherBricks(double crackedChance) {
		return pos -> Permuter.toChancedBoolean(Permuter.permute(0x9F3EDFCC2AD1A6DBL, pos), crackedChance) ? BlockStates.CRACKED_NETHER_BRICKS : BlockStates.NETHER_BRICKS;
	}

	public static CoordinateSupplier<BlockState> randomState(Block block) {
		ImmutableList<BlockState> states = block.getStateManager().getStates();
		return pos -> states.get(Permuter.nextBoundedInt(Permuter.permute(0x1254A5D35E112FBCL, pos), states.size()));
	}

	public static CoordinateSupplier<BlockState> netherWart() {
		return randomState(Blocks.NETHER_WART);
	}

	public static CoordinateSupplier<BlockState> maybeCryingObsidian(int chance) {
		return pos -> Permuter.nextBoundedInt(Permuter.permute(0xACE491D9D10891E0L, pos), chance) == 0 ? BlockStates.CRYING_OBSIDIAN : BlockStates.OBSIDIAN;
	}

	public static BlockState STAIRS(Block block, char type) {
		BlockState state = block.getDefaultState();
		switch (type) {
			case 'N': state = state.with(StairsBlock.HALF, BlockHalf.TOP);
			case 'n': state = state.with(StairsBlock.FACING, Direction.NORTH); break;
			case 'S': state = state.with(StairsBlock.HALF, BlockHalf.TOP);
			case 's': state = state.with(StairsBlock.FACING, Direction.SOUTH); break;
			case 'E': state = state.with(StairsBlock.HALF, BlockHalf.TOP);
			case 'e': state = state.with(StairsBlock.FACING, Direction.EAST); break;
			case 'W': state = state.with(StairsBlock.HALF, BlockHalf.TOP);
			case 'w': state = state.with(StairsBlock.FACING, Direction.WEST); break;
			default: throw new IllegalArgumentException("not nsew: " + type);
		}
		return state;
	}

	public static BlockState NETHER_BRICK_STAIRS(char type) {
		return STAIRS(Blocks.NETHER_BRICK_STAIRS, type);
	}

	public static BlockState RED_NETHER_BRICK_STAIRS(char type) {
		return STAIRS(Blocks.RED_NETHER_BRICK_STAIRS, type);
	}

	public static BlockState POLISHED_BLACKSTONE_STAIRS(char type) {
		return STAIRS(Blocks.POLISHED_BLACKSTONE_STAIRS, type);
	}

	public static BlockState FENCE(Block block, String connections) {
		BlockState state = block.getDefaultState();
		for (int index = 0, length = connections.length(); index < length; index++) {
			state = switch (connections.charAt(index)) {
				case 'n' -> state.with(HorizontalConnectingBlock.NORTH, Boolean.TRUE);
				case 'e' -> state.with(HorizontalConnectingBlock.EAST, Boolean.TRUE);
				case 's' -> state.with(HorizontalConnectingBlock.SOUTH, Boolean.TRUE);
				case 'w' -> state.with(HorizontalConnectingBlock.WEST, Boolean.TRUE);
				default -> throw new IllegalArgumentException(String.valueOf(connections.charAt(index)));
			};
		}
		return state;
	}

	public static BlockState NETHER_BRICK_FENCE(String connections) {
		return FENCE(Blocks.NETHER_BRICK_FENCE, connections);
	}

	public static BlockState WALL(Block block, String connections) {
		BlockState state = block.getDefaultState();
		for (int index = 0, length = connections.length(); index < length; index++) {
			state = switch (connections.charAt(index)) {
				case 'n' -> state.with(WallBlock.NORTH_SHAPE, WallShape.LOW);
				case 'e' -> state.with(WallBlock.EAST_SHAPE,  WallShape.LOW);
				case 's' -> state.with(WallBlock.SOUTH_SHAPE, WallShape.LOW);
				case 'w' -> state.with(WallBlock.WEST_SHAPE,  WallShape.LOW);
				case 'N' -> state.with(WallBlock.NORTH_SHAPE, WallShape.TALL);
				case 'E' -> state.with(WallBlock.EAST_SHAPE,  WallShape.TALL);
				case 'S' -> state.with(WallBlock.SOUTH_SHAPE, WallShape.TALL);
				case 'W' -> state.with(WallBlock.WEST_SHAPE,  WallShape.TALL);
				case 'u', 'U' -> state.with(WallBlock.UP, Boolean.TRUE);
				default -> throw new IllegalArgumentException(String.valueOf(connections.charAt(index)));
			};
		}
		return state;
	}

	public static BlockState NETHER_BRICK_WALL(String connections) {
		return WALL(Blocks.NETHER_BRICK_WALL, connections);
	}

	public static NbtList makeEntityPos(double x, double y, double z) {
		NbtList pos = new NbtList();
		pos.add(NbtDouble.of(x));
		pos.add(NbtDouble.of(y));
		pos.add(NbtDouble.of(z));
		return pos;
	}

	public static <T> List<T> readListFromNBT(NbtList listNBT, Function<? super NbtCompound, ? extends T> reader) {
		int size = listNBT.size();
		if (size == 0) return Collections.emptyList();
		List<T> list = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			list.add(reader.apply(listNBT.getCompound(index)));
		}
		return list;
	}

	public static <T> List<T> readListFromNBTCompound(NbtCompound compound, String key, Function<? super NbtCompound, ? extends T> reader) {
		NbtElement nbt = compound.get(key);
		if (nbt instanceof NbtList list) {
			if (!list.isEmpty() && list.getHeldType() == NbtElement.COMPOUND_TYPE) {
				return readListFromNBT(list, reader);
			}
		}
		return Collections.emptyList();
	}

	public static <T> NbtList writeListToNBT(List<T> list, Function<? super T, ? extends NbtCompound> writer) {
		NbtList listNBT = new NbtList();
		for (int index = 0, size = list.size(); index < size; index++) {
			listNBT.add(writer.apply(list.get(index)));
		}
		return listNBT;
	}

	public static <T> void writeListToNBTAndStoreInCompound(NbtCompound compound, String key, List<T> list, Function<? super T, ? extends NbtCompound> writer) {
		if (!list.isEmpty()) compound.put(key, writeListToNBT(list, writer));
	}

	public static <T> void doForSomeElements(RandomGenerator random, T[] elements, int desiredElements, Consumer<T> action) {
		if (desiredElements > elements.length) {
			throw new IllegalArgumentException("desiredElements (" + desiredElements + ") > elements.length (" + elements.length + ')');
		}
		if (desiredElements <= 0) {
			return;
		}
		if (desiredElements == 1) {
			action.accept(elements[random.nextInt(elements.length)]);
			return;
		}
		elements = elements.clone();
		int length = elements.length;
		for (int attempt = 0; attempt < desiredElements; attempt++) {
			int index = random.nextInt(length);
			T element = elements[index];
			elements[index] = elements[--length];
			action.accept(element);
		}
	}

	public static class PositionDirections extends BlockPos {

		public final Direction[] directions;

		public PositionDirections(int x, int y, int z, Direction... directions) {
			super(x, y, z);
			this.directions = directions;
		}

		public Direction getRandomDirection(RandomGenerator random) {
			Direction[] directions = this.directions;
			return directions[directions.length == 1 ? 0 : random.nextInt(directions.length)];
		}

		public Direction getRandomDirection() {
			Direction[] directions = this.directions;
			return directions[directions.length == 1 ? 0 : Permuter.nextBoundedInt(Permuter.permute(0x4500DBA0FF31AA96L, this), directions.length)];
		}
	}

	public static class PositionState extends BlockPos {

		public BlockState state;
		public NbtCompound blockEntityData;

		public PositionState(Vec3i vector, BlockState state, NbtCompound blockEntityData) {
			super(vector);
			this.state = state;
			this.blockEntityData = blockEntityData;
		}

		public PositionState(Vec3i vector, BlockState state) {
			this(vector, state, null);
		}

		public PositionState(NbtCompound nbt) {
			super(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
			this.state = NbtHelper.toBlockState(nbt);
			this.blockEntityData = nbt.get("BlockEntityTag") instanceof NbtCompound compound ? compound : null;
		}

		public NbtCompound writeToNBT() {
			NbtCompound nbt = new NbtCompound();
			this.writeToNBT(nbt);
			return nbt;
		}

		public void writeToNBT(NbtCompound nbt) {
			nbt.putInt("x", this.getX());
			nbt.putInt("y", this.getY());
			nbt.putInt("z", this.getZ());
			nbt.copyFrom(NbtHelper.fromBlockState(this.state));
			if (this.blockEntityData != null) nbt.put("BlockEntityTag", this.blockEntityData);
		}

		public void readFromNBT(NbtCompound nbt) {
			this.setX(nbt.getInt("x"));
			this.setY(nbt.getInt("y"));
			this.setZ(nbt.getInt("z"));
			this.state = NbtHelper.toBlockState(nbt);
			this.blockEntityData = nbt.get("BlockEntityTag") instanceof NbtCompound compound ? compound : null;
		}

		public boolean place(StructureWorldAccess world, BlockPos origin, BlockBox box) {
			BlockPos pos = this.add(origin);
			if (!box.contains(pos)) return false;
			if (!world.setBlockState(pos, this.state, Block.NOTIFY_ALL)) return false;
			if (this.blockEntityData != null) {
				BlockEntity tileEntity = WorldUtil.getBlockEntity(world, pos, BlockEntity.class);
				if (tileEntity != null) {
					NbtCompound oldNBT = tileEntity.createNbt();
					NbtCompound newNBT = oldNBT.copy().copyFrom(this.blockEntityData);
					newNBT.putInt("x", pos.getX());
					newNBT.putInt("y", pos.getY());
					newNBT.putInt("z", pos.getZ());
					if (!oldNBT.equals(newNBT)) {
						tileEntity.readNbt(newNBT);
						tileEntity.markDirty();
					}
				}
			}
			return true;
		}
	}

	public static abstract class Piece extends StructurePiece {

		public BlockPos centerPos;
		public byte variant;

		public Piece(StructurePieceType type, int x, int y, int z, BlockBox boundingBox, RandomGenerator random, int variantCount) {
			super(type, 0, boundingBox);
			this.centerPos = new BlockPos(x, y, z);
			if (variantCount > 0) this.variant = (byte)(random.nextInt(variantCount));
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.variant = nbt.getByte("variant");
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			nbt.putByte("var", this.variant);
		}

		public Coordinator coordinator(StructureWorldAccess world, BlockBox box) {
			return Coordinator.forWorld(world, Block.NOTIFY_ALL).inBox(box, false).translate(this.centerPos, false);
		}

		@Override
		public void translate(int x, int y, int z) {
			super.translate(x, y, z);
			this.centerPos = this.centerPos.add(x, y, z);
		}
	}

	public static class MainBuildingPiece extends Piece {

		public static final PositionDirections[] CHEST_POSITIONS = {
			new PositionDirections(- 2, 1, -10, Direction.SOUTH),
			new PositionDirections(- 5, 1, -10, Direction.SOUTH, Direction.EAST),
			new PositionDirections(- 5, 1, - 6, Direction.EAST),
			new PositionDirections(- 6, 1, - 5, Direction.SOUTH),
			new PositionDirections(-10, 1, - 5, Direction.SOUTH, Direction.EAST),
			new PositionDirections(-10, 1, - 2, Direction.EAST),
			new PositionDirections(- 2, 1,  10, Direction.NORTH),
			new PositionDirections(- 5, 1,  10, Direction.NORTH, Direction.EAST),
			new PositionDirections(- 5, 1,   6, Direction.EAST),
			new PositionDirections(- 6, 1,   5, Direction.NORTH),
			new PositionDirections(-10, 1,   5, Direction.NORTH, Direction.EAST),
			new PositionDirections(-10, 1,   2, Direction.EAST),
			new PositionDirections(  2, 1, -10, Direction.SOUTH),
			new PositionDirections(  5, 1, -10, Direction.SOUTH, Direction.WEST),
			new PositionDirections(  5, 1, - 6, Direction.WEST),
			new PositionDirections(  6, 1, - 5, Direction.SOUTH),
			new PositionDirections( 10, 1, - 5, Direction.SOUTH, Direction.WEST),
			new PositionDirections( 10, 1, - 2, Direction.WEST),
			new PositionDirections(  2, 1,  10, Direction.NORTH),
			new PositionDirections(  5, 1,  10, Direction.NORTH, Direction.WEST),
			new PositionDirections(  5, 1,   6, Direction.WEST),
			new PositionDirections(  6, 1,   5, Direction.NORTH),
			new PositionDirections( 10, 1,   5, Direction.NORTH, Direction.WEST),
			new PositionDirections( 10, 1,   2, Direction.WEST),
		};

		public static final PositionDirections[] ARCH_POSITIONS = {
			new PositionDirections(-6, 1, -8, Direction.EAST),
			new PositionDirections(-8, 1, -6, Direction.SOUTH),
			new PositionDirections(-6, 1,  8, Direction.EAST),
			new PositionDirections(-8, 1,  6, Direction.NORTH),
			new PositionDirections( 6, 1, -8, Direction.WEST),
			new PositionDirections( 8, 1, -6, Direction.SOUTH),
			new PositionDirections( 6, 1,  8, Direction.WEST),
			new PositionDirections( 8, 1,  6, Direction.NORTH),
		};

		public static final Vec3i[] SPAWN_POSITIONS = {
			new Vec3i(  0,  1, -13),
			new Vec3i(  0,  1, - 8),
			new Vec3i(- 4,  1, - 8),
			new Vec3i(- 4,  1, - 4),
			new Vec3i(- 8,  1, - 4),
			new Vec3i(- 8,  1,   0),
			new Vec3i(-13,  1,   0),
			new Vec3i(  0,  6, -13),
			new Vec3i(  0, 10, - 5),
			new Vec3i(- 5, 10, - 5),
			new Vec3i(- 5, 10,   0),
			new Vec3i(-13,  6,   0),
			new Vec3i(  0,  1,  13),
			new Vec3i(  0,  1,   8),
			new Vec3i(- 4,  1,   8),
			new Vec3i(- 4,  1,   4),
			new Vec3i(- 8,  1,   4),
			new Vec3i(  0,  6,  13),
			new Vec3i(  0, 10,   5),
			new Vec3i(- 5, 10,   5),
			new Vec3i(  4,  1, - 8),
			new Vec3i(  4,  1, - 4),
			new Vec3i(  8,  1, - 4),
			new Vec3i(  8,  1,   0),
			new Vec3i( 13,  1,   0),
			new Vec3i(  5, 10, - 5),
			new Vec3i(  5, 10,   0),
			new Vec3i( 13,  6,   0),
			new Vec3i(  4,  1,   8),
			new Vec3i(  4,  1,   4),
			new Vec3i(  8,  1,   4),
			new Vec3i(  5, 10,   5),
		};

		public static final RandomList<String> CHEST_LOOT_TABLES = (
			new RandomList<String>(4)
			.addSelf(LootTables.NETHER_BRIDGE_CHEST.toString().intern(), 100.0F)
			.addSelf(LootTables.RUINED_PORTAL_CHEST.toString().intern(), 50.0F)
			.addSelf(LootTables.BASTION_OTHER_CHEST.toString().intern(), 25.0F)
			.addSelf(LootTables.BASTION_BRIDGE_CHEST.toString().intern(), 10.0F)
		);

		public final double crackedChance;
		public final List<PositionState> decorations;
		public final List<NbtCompound> entities;

		public MainBuildingPiece(StructurePieceType type, int x, int y, int z, double crackedChance, RandomGenerator random) {
			super(type, x, y, z, new BlockBox(x - 16, y, z - 16, x + 16, y + 12, z + 16), random, 2);
			this.crackedChance = crackedChance;
			int chestCount = Integer.bitCount(random.nextInt(1 << 4));
			int bannerCount = Integer.bitCount(random.nextInt(1 << 6));
			int armorStandCount = Integer.bitCount(random.nextInt(1 << 2));
			int entityCount = Integer.bitCount(random.nextInt());
			this.decorations = new ArrayList<>(chestCount + bannerCount);
			this.entities = new ArrayList<>(armorStandCount + entityCount);
			doForSomeElements(random, CHEST_POSITIONS, chestCount, chestPos -> {
				NbtCompound chestNBT = new NbtCompound();
				chestNBT.putString("LootTable", CHEST_LOOT_TABLES.getRandomElement(random));
				chestNBT.putLong("LootTableSeed", random.nextLong());
				this.decorations.add(new PositionState(
					chestPos,
					random.nextBoolean()
						? Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, chestPos.getRandomDirection(random))
						: Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, chestPos.getRandomDirection(random)),
					chestNBT
				));
			});
			doForSomeElements(random, ARCH_POSITIONS, bannerCount + armorStandCount, new Consumer<PositionDirections>() {

				public int index;

				@Override
				public void accept(PositionDirections archPosition) {
					if (this.index++ < armorStandCount) {
						NbtCompound entityNBT = new NbtCompound();
						entityNBT.putString("id", "armor_stand");
						entityNBT.put("Pos", makeEntityPos(archPosition.getX() + 0.5D, archPosition.getY(), archPosition.getZ() + 0.5D));
						NbtList rotation = new NbtList();
						rotation.add(NbtFloat.of(archPosition.getRandomDirection(random).asRotation()));
						rotation.add(NbtFloat.ZERO);
						entityNBT.put("Rotation", rotation);
						NbtList armorItemsNBT = new NbtList();
						int numberOfArmorItems = random.nextInt(5);
						for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
							NbtCompound itemNBT = new NbtCompound();
							if (armorIndex >= numberOfArmorItems) {
								Item item;
								switch (armorIndex) {
									case 0: item = Items.GOLDEN_BOOTS;      break;
									case 1: item = Items.GOLDEN_LEGGINGS;   break;
									case 2: item = Items.GOLDEN_CHESTPLATE; break;
									case 3: item = Items.GOLDEN_HELMET;     break;
									default: throw new AssertionError(armorIndex);
								}
								ItemStack stack = new ItemStack(item, 1);
								stack.setDamage(random.nextInt(stack.getMaxDamage()));
								stack.writeNbt(itemNBT);
							}
							armorItemsNBT.add(itemNBT);
						}
						entityNBT.put("ArmorItems", armorItemsNBT);
						MainBuildingPiece.this.entities.add(entityNBT);
					}
					else { //placing banner instead of armor stand.
						boolean invertColors = random.nextBoolean();
						NbtCompound bannerNBT = new NbtCompound();
						NbtList patterns = new NbtList();
						patterns.add(this.pattern("cbo", true, invertColors));
						patterns.add(this.pattern("bt", true, invertColors));
						patterns.add(this.pattern("tt", true, invertColors));
						patterns.add(this.pattern("bts", false, invertColors));
						patterns.add(this.pattern("tts", false, invertColors));
						patterns.add(this.pattern("mr", false, invertColors));
						patterns.add(this.pattern("mc", true, invertColors));
						patterns.add(this.pattern("bo", 15));
						bannerNBT.put("Patterns", patterns);
						MainBuildingPiece.this.decorations.add(new PositionState(
							archPosition.up(2),
							(invertColors ? Blocks.MAGENTA_WALL_BANNER : Blocks.PURPLE_WALL_BANNER)
								.getDefaultState()
								.with(WallBannerBlock.FACING, archPosition.getRandomDirection(random)),
							bannerNBT
						));
					}
				}

				public NbtCompound pattern(String patternName, boolean magenta, boolean invert) {
					return this.pattern(patternName, magenta == invert ? 10 : 2);
				}

				public NbtCompound pattern(String patternName, int color) {
					NbtCompound nbt = new NbtCompound();
					nbt.putString("Pattern", patternName);
					nbt.putByte("Color", (byte)(color));
					return nbt;
				}
			});
		}

		public MainBuildingPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.centerPos = new BlockPos(
				(this.boundingBox.getMinX() + this.boundingBox.getMaxX() + 1) >> 1,
				this.boundingBox.getMinY(),
				(this.boundingBox.getMinZ() + this.boundingBox.getMaxZ() + 1) >> 1
			);
			this.crackedChance = nbt.getDouble("cracked_chance");
			this.decorations = readListFromNBTCompound(nbt, "decorations", PositionState::new);
			this.entities = readListFromNBTCompound(nbt, "entities", Function.identity());
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.putDouble("cracked_chance", this.crackedChance);
			writeListToNBTAndStoreInCompound(nbt, "decorations", this.decorations, PositionState::writeToNBT);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			Coordinator rotate8 = root.flip2X().rotate4x90();
			Coordinator floorFlip = root.flip2Z();
			CoordinateSupplier<BlockState> netherBricks = netherBricks(this.crackedChance);

			//empty out existing areas
			root.setBlockStateCuboid(-16, 2, -16, 16, 12, 16, BlockStates.AIR);
			//ground level
			floorFlip.setBlockStateCuboid(-1, 1, -14, 1, 1, -11, BlockStates.AIR);
			floorFlip.setBlockStateCuboid(-6, 1, -10, 6, 1, -7, BlockStates.AIR);
			floorFlip.setBlockStateCuboid(-10, 1, -6, 10, 1, -2, BlockStates.AIR);
			root.setBlockStateCuboid(-14, 1, -1, 14, 1, 1, BlockStates.AIR);
			//floor
			root.setBlockStateCuboid(-15, 0, -6, 15, 0, 6, netherBricks);
			floorFlip.setBlockStateCuboid(-6, 0, -15, 6, 0, -7, netherBricks);
			//red nether brick pillars
			rotate4.setBlockStateCuboid(-10, 0, -10, -7, 6, -7, BlockStates.RED_NETHER_BRICKS);
			rotate4.setBlockStateCuboid(-9, 7, -9, -7, 7, -7, BlockStates.RED_NETHER_BRICKS);
			rotate4.setBlockStateCuboid(-8, 8, -8, -7, 8, -7, BlockStates.RED_NETHER_BRICKS);
			//platform under lava
			rotate8.setBlockStateCuboid(-6, 1, -15, -2, 1, -11, netherBricks);
			//main outer wall
			rotate8.setBlockStateCuboid(-6, 2, -11, -2, 5, -11, netherBricks);
			//entry wall
			rotate8.setBlockStateCuboid(-2, 2, -15, -2, 4, -11, netherBricks);
			//pyramid
			rotate8.setBlockStateLine(-3, 6, -11, -1, 0, 0, 9, netherBricks);
			rotate8.setBlockState(-2, 6, -11, BlockStates.CHISELED_NETHER_BRICKS);
			rotate8.setBlockStateLine(-2, 7, -10, -1, 0, 0, 8, netherBricks);
			rotate8.setBlockStateLine(-2, 8, -9, -1, 0, 0, 7, netherBricks);
			rotate8.setBlockStateLine(-2, 9, -8, -1, 0, 0, 6, netherBricks);
			rotate8.setBlockStateLine(-3, 10, -7, -1, 0, 0, 5, netherBricks);
			rotate8.setBlockState(-2, 10, -7, BlockStates.CHISELED_NETHER_BRICKS);
			//pyramid decorations
			rotate8.setBlockStateLine(-2, 7, -11, 0, 1, 1, 4, NETHER_BRICK_STAIRS('s'));
			rotate4.setBlockStateLine(-11, 6, -11, 1, 1, 1, 5, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockStateLine(-11, 7, -11, 1, 1, 1, 4, Blocks.NETHER_BRICK_FENCE.getDefaultState());
			rotate4.setBlockState(-7, 11, -7, NETHER_BRICK_FENCE("es"));
			rotate8.setBlockStateLine(-6, 11, -7, 1, 0, 0, 4, NETHER_BRICK_FENCE("ew"));
			rotate8.setBlockState(-2, 11, -7, NETHER_BRICK_FENCE("w"));
			//roof
			root.setBlockStateCuboid(-7, 9, -7, 7, 9, 7, BlockStates.RED_NETHER_BRICKS);
			//small walkway connecting the two sets of stairs
			rotate4.setBlockStateCuboid(-2, 5, -14,2, 5, -12, BlockStates.RED_NETHER_BRICKS);
			//lower stairs
			rotate8.stack(0, 0, 1, 3).setBlockStateLine(-6, 2, -14, 1, 1, 0, 4, RED_NETHER_BRICK_STAIRS('e'));
			//upper stairs
			rotate4.stack(1, 0, 0, 3).setBlockStateLine(-1, 6, -11, 0, 1, 1, 4, RED_NETHER_BRICK_STAIRS('s'));
			//outer wall
			rotate8.setBlockState(-6, 2, -15, BlockStates.CHISELED_NETHER_BRICKS);
			rotate8.genericLine(-6, 2, -15, 1, 0, 0, 5, (coordinator, x, y, z, index) -> {
				if (index != 0 && index != 2) { //micro-optimization: these blocks will be overridden.
					coordinator.setBlockStateLine(x, y, z, 0, 1, 0, index + 1, netherBricks);
				}
			});
			rotate4.setBlockStateLine(-1, 6, -15, 1, 0, 0, 3, netherBricks);
			rotate4.setBlockStateLine(-1, 1, -15, 1, 0, 0, 3, NETHER_BRICK_STAIRS('n'));
			rotate4.setBlockStateLine(-1, 5, -15, 1, 0, 0, 3, NETHER_BRICK_STAIRS('S'));
			//railing
			rotate8.setBlockStateLine(-6, 3, -15, 1, 1, 0, 4, NETHER_BRICK_STAIRS('e'));
			rotate8.setBlockState(-2, 7, -15, NETHER_BRICK_FENCE("e"));
			rotate4.setBlockStateLine(-1, 7, -15, 1, 0, 0, 3, NETHER_BRICK_FENCE("ew"));
			//fences to make the lava visible
			rotate8.setBlockStateLine(-4, 2, -15, 0, 1, 0,
				NETHER_BRICK_FENCE("ew"),
				NETHER_BRICK_FENCE("ew"),
				NETHER_BRICK_STAIRS('S')
			);
			rotate8.setBlockStateLine(-2, 1, -13, 0, 1, 0,
				NETHER_BRICK_STAIRS('w'),
				NETHER_BRICK_FENCE("ns"),
				NETHER_BRICK_FENCE("ns"),
				NETHER_BRICK_STAIRS('W')
			);
			rotate8.setBlockStateLine(-4, 1, -11, 0, 1, 0,
				NETHER_BRICK_STAIRS('n'),
				NETHER_BRICK_FENCE("ew"),
				NETHER_BRICK_FENCE("ew"),
				NETHER_BRICK_STAIRS('N')
			);
			//lava under stairs
			rotate8.stack(0, 0, 1, 3).genericLine(-5, 2, -14, 1, 0, 0, 3, (coordinator, x, y, z, index) -> {
				coordinator.setBlockStateLine(x, y, z, 0, 1, 0, index + 1, BlockStates.LAVA);
			});
			//interior sloped ceiling
			rotate8.stack(1, 0, 0, 3).setBlockStateLine(-5, 6, -10, 0, 1, 1, 3, RED_NETHER_BRICK_STAIRS('N'));
			rotate8.setBlockStateLine(-2, 6, -10, 0, 1, 1, 3, netherBricks);
			rotate8.setBlockStateLine(-2, 5, -10, 0, 1, 1, 4, NETHER_BRICK_STAIRS('N'));
			rotate4.stack(1, 0, 0, 3).setBlockStateLine(-1, 5, -11, 0, 1, 1, 4, RED_NETHER_BRICK_STAIRS('N'));
			//ceiling slab decorations
			rotate8.setBlockState(-5, 8, -6, NETHER_BRICK_STAIRS('W'));
			rotate4.setBlockStateLine(-4, 8, -6, 1, 0, 0, 9, Blocks.NETHER_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
			//interior arches
			rotate8.setBlockState(-6, 1, -10, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockState(-6, 1, -6, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockStateLine(-6, 2, -6, 0, 1, 0, 7, netherBricks);
			if ((this.variant & 1) != 0) {
				rotate8.setBlockStateLine(-6, 2, -10, 0, 1, 0, 4, netherBricks);
				rotate8.stack(0, 0, 1, 2).setBlockStateLine(-6, 6, -10, 0, 1, 1, 3, netherBricks);
				rotate8.setBlockStateLine(-6, 5, -9, 0, 1, 1, 3, NETHER_BRICK_STAIRS('N'));
			}
			else {
				rotate8.setBlockStateLine(-6, 2, -10, 0, 1, 0, 5, netherBricks);
				rotate8.setBlockStateLine(-6, 5, -9, 0, 1, 0, 3, netherBricks);
				rotate8.setBlockStateLine(-6, 5, -8, 0, 1, 0, 4, netherBricks);
				rotate8.setBlockStateLine(-6, 5, -7, 0, 1, 0, 4, netherBricks);
				rotate8.setBlockState(-6, 4, -9, NETHER_BRICK_STAIRS('N'));
				rotate8.setBlockState(-6, 4, -7, NETHER_BRICK_STAIRS('S'));
			}
			//exterior arches
			rotate8.setBlockState(-7, 2, -11, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockState(-11, 2, -11, BlockStates.CHISELED_NETHER_BRICKS);
			rotate8.setBlockStateLine(-7, 3, -11, 0, 1, 0, 3, netherBricks);
			rotate4.setBlockStateLine(-11, 3, -11, 0, 1, 0, 3, netherBricks);
			rotate8.setBlockState(-8, 5, -11, NETHER_BRICK_STAIRS('E'));
			rotate8.setBlockState(-10, 5, -11, NETHER_BRICK_STAIRS('W'));
			//decorations
			if (!this.decorations.isEmpty()) {
				this.decorations.removeIf(positionState -> positionState.place(world, this.centerPos, chunkBox));
			}
			//entities
			for (Iterator<NbtCompound> iterator = this.entities.iterator(); iterator.hasNext();) {
				NbtCompound nbt = iterator.next();
				NbtList posNBT = nbt.getList("Pos", NbtElement.DOUBLE_TYPE);
				if (posNBT.size() != 3) {
					BigGlobeMod.LOGGER.warn("Portal temple main building entity lacks position: " + nbt);
					iterator.remove();
					continue;
				}
				double x = posNBT.getDouble(0) + this.centerPos.getX();
				double y = posNBT.getDouble(1) + this.centerPos.getY();
				double z = posNBT.getDouble(2) + this.centerPos.getZ();
				BlockPos pos = new BlockPos(x, y, z);
				if (chunkBox.contains(pos)) {
					nbt.put("Pos", makeEntityPos(x, y, z));
					EntityType.getEntityFromNbt(nbt, world.toServerWorld()).ifPresent(entity -> {
						if (entity instanceof MobEntity mob) {
							mob.initialize(
								world,
								world.getLocalDifficulty(pos),
								SpawnReason.STRUCTURE,
								null,
								nbt
							);
						}
						world.spawnEntityAndPassengers(entity);
					});
					iterator.remove();
				}
			}
		}
	}

	public static class PortalPiece extends Piece {

		public static final BlockState
			NETHER_PORTAL_X = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, Axis.X),
			NETHER_PORTAL_Z = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, Axis.Z);

		public PortalPiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, new BlockBox(x - 4, y, z - 4, x + 4, y + 17, z + 4), random, 16);
		}

		public PortalPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.centerPos = new BlockPos(
				(this.boundingBox.getMinX() + this.boundingBox.getMaxX() + 1) >> 1,
				this.boundingBox.getMinY(),
				(this.boundingBox.getMinZ() + this.boundingBox.getMaxZ() + 1) >> 1
			);
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			Coordinator rotate8 = root.flip2X().rotate4x90();

			//empty out existing areas
			root.setBlockStateCuboid(-4, 3, -4, 4, 17, 4, BlockStates.AIR);
			//obsidian
			Coordinator obsidianStack = rotate4.stack(0, 11, 0, 2);
			obsidianStack.setBlockStateLine(-2, 0, -2, 1, 0, 0, 4, BlockStates.OBSIDIAN);
			obsidianStack.setBlockState(-1, 0, -1, maybeCryingObsidian(4));
			obsidianStack.setBlockState(0, 0, -1, maybeCryingObsidian(2));
			root.setBlockStateLine(0, 0, 0, 0, 11, 0, 2, BlockStates.CRYING_OBSIDIAN);
			rotate4.setBlockStateLine(-2, 1, -2, 0, 1, 0, 10, BlockStates.OBSIDIAN);
			//portal blocks
			if ((this.variant & 1) != 0) root.setBlockStateCuboid(-1, 1, -2, 1, 10, -2, NETHER_PORTAL_X);
			if ((this.variant & 2) != 0) root.setBlockStateCuboid(2, 1, -1, 2, 10, 1, NETHER_PORTAL_Z);
			if ((this.variant & 4) != 0) root.setBlockStateCuboid(-1, 1, 2, 1, 10, 2, NETHER_PORTAL_X);
			if ((this.variant & 8) != 0) root.setBlockStateCuboid(-2, 1, -1, -2, 10, 1, NETHER_PORTAL_Z);
			//decorations
			rotate4.setBlockStateLine(-1, 0, -3, 1, 0, 0, 3, NETHER_BRICK_STAIRS('s'));
			rotate8.setBlockStateLine(-2, 0, -3, 0, 1, 0,
				BlockStates.CHISELED_NETHER_BRICKS,
				NETHER_BRICK_STAIRS('s'),
				NETHER_BRICK_WALL("SU"),
				NETHER_BRICK_FENCE("s"),
				NETHER_BRICK_WALL("su"),
				NETHER_BRICK_STAIRS('S'),
				BlockStates.NETHER_BRICKS,
				NETHER_BRICK_FENCE("sw"),
				NETHER_BRICK_FENCE("sw"),
				NETHER_BRICK_STAIRS('W'),
				BlockStates.NETHER_BRICKS,
				BlockStates.CHISELED_NETHER_BRICKS
			);
			rotate4.setBlockState(-3, 0, -3, NETHER_BRICK_FENCE("es"));
			rotate4.setBlockStateLine(-3, 6, -3, 0, 1, 0, 4, BlockStates.NETHER_BRICKS);
			rotate4.setBlockStateLine(-3, 10, -3, 0, 1, 0,
				NETHER_BRICK_WALL("ESU"),
				NETHER_BRICK_FENCE("es")
			);
			rotate8.setBlockStateLine(-1, 6, -3, 0, 1, 0,
				NETHER_BRICK_STAIRS('W'),
				NETHER_BRICK_STAIRS('e'),
				null,
				null,
				NETHER_BRICK_STAIRS('W')
			);
			rotate4.setBlockStateLine(0, 7, -3, 0, 1, 0,
				BlockStates.CHISELED_NETHER_BRICKS,
				BlockStates.NETHER_BRICKS,
				NETHER_BRICK_WALL(""),
				BlockStates.NETHER_BRICKS
			);

			rotate4.setBlockStateLine(-1, 11, -3, 1, 0, 0, 3, NETHER_BRICK_STAIRS('s'));
			rotate4.setBlockStateLine(-2, 12, -2, 0, 1, 0, 3, BlockStates.NETHER_BRICKS);
			rotate4.translate(-2, 12, -2).rotate4x90().setBlockStateLine(0, 0, -1, 0, 1, 0,
				NETHER_BRICK_WALL("SU"),
				NETHER_BRICK_FENCE("s")
			);
			rotate4.setBlockStateLine(-2, 15, -2, 0, 1, 0, NETHER_BRICK_WALL(""), NETHER_BRICK_FENCE(""));
		}
	}

	public static abstract class DecorationPiece extends Piece {

		public DecorationPiece(StructurePieceType type, int x, int y, int z, RandomGenerator random, int variantCount) {
			super(type, x, y, z, null, random, variantCount);
			this.initBoundingBox(x, y, z, random);
		}

		public DecorationPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.centerPos = new BlockPos(
				(this.boundingBox.getMinX() + this.boundingBox.getMaxX() + 1) >> 1,
				this.isCeiling() ? this.boundingBox.getMaxY() : this.boundingBox.getMinY(),
				(this.boundingBox.getMinZ() + this.boundingBox.getMaxZ() + 1) >> 1
			);
		}

		public void initBoundingBox(int x, int y, int z, RandomGenerator random) {
			int minX, minY, minZ, maxX, maxY, maxZ;
			int radius = this.getRadius();
			if (this.isCeiling()) {
				x += nextUniformIntInclusive(random, 4 - radius);
				y += 8;
				z += nextUniformIntInclusive(random, 4 - radius);
				minY = y - this.getHeight() + 1;
				maxY = y;
			}
			else {
				int magnitude = nextUniformIntInclusive(random, 7 - radius);
				int side = nextUniformIntInclusive(random, 2 - radius);
				if (random.nextBoolean()) {
					x += magnitude;
					z += side;
				}
				else {
					z += magnitude;
					x += side;
				}
				y += 1;
				minY = y;
				maxY = y + this.getHeight() - 1;
			}
			minX = x - radius;
			minZ = z - radius;
			maxX = x + radius;
			maxZ = z + radius;
			this.boundingBox = new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
			this.centerPos = new BlockPos(x, y, z);
		}


		/** returns a random int in the range [-range, range]. */
		public static int nextUniformIntInclusive(RandomGenerator random, int range) {
			return random.nextInt((range << 1) | 1) - range;
		}

		public abstract int getRadius();

		public abstract int getHeight();

		public boolean isCeiling() {
			return false;
		}
	}

	public static class WellPiece extends DecorationPiece {

		public WellPiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, random, 2);
		}

		public WellPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public int getRadius() {
			return 1;
		}

		@Override
		public int getHeight() {
			return 1;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			rotate4.setBlockState(-1, 0, -1, (this.variant & 1) != 0 ? BlockStates.CHISELED_NETHER_BRICKS : BlockStates.NETHER_BRICKS);
			rotate4.setBlockState(0, 0, -1, BlockStates.NETHER_BRICKS);
			root.setBlockState(0, 0, 0, BlockStates.LAVA);
		}
	}

	public static class FarmPiece extends DecorationPiece {

		public FarmPiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, random, 2);
		}

		public FarmPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public int getRadius() {
			return 2;
		}

		@Override
		public int getHeight() {
			return 5;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			//corners
			rotate4.setBlockState(-2, 0, -2, BlockStates.CHISELED_NETHER_BRICKS);
			//bottom stairs
			rotate4.setBlockStateLine(-1, 0, -2, 1, 0, 0, 3, NETHER_BRICK_STAIRS('s'));
			//fences
			rotate4.setBlockStateLine(-2, 1, -2, 0, 1, 0, 2, NETHER_BRICK_FENCE(""));
			//top stairs
			rotate4.setBlockState(-2, 3, -2, NETHER_BRICK_STAIRS('s').with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
			rotate4.setBlockStateLine(-1, 3, -2, 1, 0, 0, 3, NETHER_BRICK_STAIRS('s'));
			//slab roof
			root.setBlockStateCuboid(-1, 4, -1, 1, 4, 1, Blocks.NETHER_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));
			//soul sand
			root.setBlockStateCuboid(-1, 0, -1, 1, 0, 1, BlockStates.SOUL_SAND);
			//nether wart
			root.setBlockStateCuboid(-1, 1, -1, 1, 1, 1, netherWart());
			//lighting
			root.setBlockStateCuboid(-1, 3, -1, 1, 3, 1, ((this.variant & 1) != 0 ? Blocks.SHROOMLIGHT : Blocks.GLOWSTONE).getDefaultState());
		}
	}

	public static class TablePiece extends DecorationPiece {

		public static final PositionDirections[] SMALL_POSITIONS = {
			new PositionDirections(-1, 1, -1, Direction.WEST, Direction.NORTH),
			new PositionDirections(-1, 1,  0, Direction.WEST),
			new PositionDirections(-1, 1,  1, Direction.WEST, Direction.SOUTH),
			new PositionDirections( 0, 1, -1, Direction.NORTH),
			new PositionDirections( 0, 1,  1, Direction.SOUTH),
			new PositionDirections( 1, 1, -1, Direction.EAST, Direction.NORTH),
			new PositionDirections( 1, 1,  0, Direction.EAST),
			new PositionDirections( 1, 1,  1, Direction.EAST, Direction.SOUTH),
		};
		public static final PositionDirections[] BIG_POSITIONS = {
			new PositionDirections(-2, 1, -2, Direction.WEST, Direction.NORTH),
			new PositionDirections(-2, 1, -1, Direction.WEST),
			new PositionDirections(-2, 1,  0, Direction.WEST),
			new PositionDirections(-2, 1,  1, Direction.WEST),
			new PositionDirections(-2, 1,  2, Direction.WEST, Direction.SOUTH),
			new PositionDirections(-1, 1, -2, Direction.NORTH),
			new PositionDirections(-1, 1,  2, Direction.SOUTH),
			new PositionDirections( 0, 1, -2, Direction.NORTH),
			new PositionDirections( 0, 1,  2, Direction.SOUTH),
			new PositionDirections( 1, 1, -2, Direction.NORTH),
			new PositionDirections( 1, 1,  2, Direction.SOUTH),
			new PositionDirections( 2, 1, -2, Direction.EAST, Direction.NORTH),
			new PositionDirections( 2, 1, -1, Direction.EAST),
			new PositionDirections( 2, 1,  0, Direction.EAST),
			new PositionDirections( 2, 1,  1, Direction.EAST),
			new PositionDirections( 2, 1,  2, Direction.EAST, Direction.SOUTH),
		};

		public final List<PositionState> decorationBlocks;

		public TablePiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, random, 4);
			int count;
			PositionDirections[] availablePositions;
			if ((this.variant & 1) != 0) {
				count = 1 + random.nextInt(2) + random.nextInt(2);
				availablePositions = BIG_POSITIONS;
			}
			else {
				count = random.nextInt(2);
				availablePositions = SMALL_POSITIONS;
			}
			this.decorationBlocks = new ArrayList<>(count);
			doForSomeElements(random, availablePositions, count, chosenPosition -> {
				BlockState state;
				switch (random.nextInt(1 + 2 + 3 + 4)) {
					case 0 -> { //anvil or grindstone
						if (random.nextBoolean()) {
							Block block = switch (random.nextInt(3)) {
								case 0 -> Blocks.ANVIL;
								case 1 -> Blocks.CHIPPED_ANVIL;
								case 2 -> Blocks.DAMAGED_ANVIL;
								default -> throw new AssertionError();
							};
							state = block.getDefaultState();
						}
						else {
							state = Blocks.GRINDSTONE.getDefaultState().with(WallMountedBlock.FACE, WallMountLocation.FLOOR);
						}
						state = state.with(HorizontalFacingBlock.FACING, Permuter.choose(random, Directions.HORIZONTAL));
					}
					case 1, 2 -> { //smithing table or stonecutter
						if (random.nextBoolean()) {
							state = Blocks.STONECUTTER.getDefaultState().with(HorizontalFacingBlock.FACING, Permuter.choose(random, Directions.HORIZONTAL));
						}
						else {
							state = Blocks.SMITHING_TABLE.getDefaultState();
						}
					}
					case 3, 4, 5 -> { //furnace
						Block block = switch (random.nextInt(4)) {
							case 0, 1 -> Blocks.FURNACE;
							case 2 -> Blocks.SMOKER;
							case 3 -> Blocks.BLAST_FURNACE;
							default -> throw new AssertionError();
						};
						state = block.getDefaultState().with(HorizontalFacingBlock.FACING, chosenPosition.getRandomDirection(random));
					}
					default -> { //crafting table
						state = Blocks.CRAFTING_TABLE.getDefaultState();
					}
				}
				this.decorationBlocks.add(new PositionState(chosenPosition, state));
			});
		}

		public TablePiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.decorationBlocks = readListFromNBTCompound(nbt, "decorations", PositionState::new);
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			writeListToNBTAndStoreInCompound(nbt, "decorations", this.decorationBlocks, PositionState::writeToNBT);
		}

		@Override
		public int getRadius() {
			return (this.variant & 1) + 1;
		}

		@Override
		public int getHeight() {
			return 2;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			if ((this.variant & 1) != 0) {
				rotate4.setBlockState(-2, 0, -2, POLISHED_BLACKSTONE_STAIRS('S').with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
				rotate4.setBlockStateLine(-1, 0, -2, 1, 0, 0, 3, POLISHED_BLACKSTONE_STAIRS('S'));
				rotate4.setBlockStateLine(-1, 0, -1, 1, 0, 0, 2, Blocks.POLISHED_BLACKSTONE.getDefaultState());
			}
			else {
				rotate4.setBlockState(-1, 0, -1, POLISHED_BLACKSTONE_STAIRS('S').with(StairsBlock.SHAPE, StairShape.OUTER_LEFT));
				rotate4.setBlockState(0, 0, -1, POLISHED_BLACKSTONE_STAIRS('S'));
			}
			root.setBlockState(0, 0, 0, ((this.variant & 2) != 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE).getDefaultState());
			if (!this.decorationBlocks.isEmpty()) {
				this.decorationBlocks.removeIf(state -> state.place(world, this.centerPos, chunkBox));
			}
		}
	}

	public static class FurnacePiece extends DecorationPiece {

		public FurnacePiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, random, 4);
		}

		public FurnacePiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public void initBoundingBox(int x, int y, int z, RandomGenerator random) {
			int magnitude = nextUniformIntInclusive(random, (this.variant & 2) != 0 ? 3 : 6);
			int side = nextUniformIntInclusive(random, 1);
			if (random.nextBoolean()) {
				x += magnitude;
				z += side;
			}
			else {
				z += magnitude;
				x += side;
			}
			y += 1;
			this.boundingBox = new BlockBox(x - 1, y, z - 1, x + 1, y + 7, z + 1);
			this.centerPos = new BlockPos(x, y, z);
		}

		@Override
		public int getRadius() {
			return 1;
		}

		@Override
		public int getHeight() {
			return (this.variant & 2) != 0 ? 8 : 4;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox);
			Coordinator rotate4 = root.rotate4x90();
			rotate4.setBlockState(-1, 0, -1, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockState(0, 0, -1, NETHER_BRICK_STAIRS('s'));
			if ((this.variant & 1) != 0) {
				root.setBlockState(0, 0, 0, BlockStates.SOUL_SAND);
				root.setBlockState(0, 1, 0, Blocks.SOUL_FIRE.getDefaultState());
			}
			else {
				root.setBlockState(0, 0, 0, BlockStates.NETHERRACK);
				root.setBlockState(0, 1, 0, Blocks.FIRE.getDefaultState());
			}
			rotate4.setBlockState(-1, 1, -1, BlockStates.NETHER_BRICKS);
			rotate4.setBlockState(0, 2, -1, BlockStates.NETHER_BRICKS);
			rotate4.setBlockState(-1, 2, -1, NETHER_BRICK_STAIRS('s').with(StairsBlock.SHAPE, StairShape.INNER_LEFT));
			if ((this.variant & 2) != 0) {
				rotate4.setBlockStateLine(0, 3, -1, 0, 1, 0, 5, BlockStates.NETHER_BRICKS);
			}
			else {
				rotate4.setBlockState(0, 3, -1, NETHER_BRICK_STAIRS('s'));
			}
		}
	}

	public static class SpawnerPiece extends DecorationPiece {

		public SpawnerPiece(StructurePieceType type, int x, int y, int z, RandomGenerator random) {
			super(type, x, y, z, random, 1);
		}

		public SpawnerPiece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
		}

		@Override
		public int getRadius() {
			return 1;
		}

		@Override
		public int getHeight() {
			return 5;
		}

		@Override
		public boolean isCeiling() {
			return true;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			Coordinator root = this.coordinator(world, chunkBox).translate(0, -3, 0);
			Coordinator rotate4 = root.rotate4x90();

			rotate4.setBlockStateLine(-1, 2, -1, 0, 1, 0, 2, NETHER_BRICK_FENCE(""));
			rotate4.setBlockState(-1, 1, -1, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockState(0, 1, -1, NETHER_BRICK_STAIRS('S'));
			root.setBlockState(0, 1, 0, BlockStates.NETHER_BRICKS);
			rotate4.setBlockState(-1, 0, -1, NETHER_BRICK_WALL(""));
			rotate4.setBlockState(-1, -1, -1, BlockStates.CHISELED_NETHER_BRICKS);
			rotate4.setBlockState(0, -1, -1, NETHER_BRICK_STAIRS('s'));
			root.setBlockState(0, -1, 0, BlockStates.NETHER_BRICKS);
			root.setBlockState(0, 0, 0, Blocks.SPAWNER.getDefaultState());
			root.getBlockEntity(0, 0, 0, BlockEntityType.MOB_SPAWNER, (pos, spawner) -> spawner.getLogic().setEntityId(EntityType.BLAZE));
		}
	}
}