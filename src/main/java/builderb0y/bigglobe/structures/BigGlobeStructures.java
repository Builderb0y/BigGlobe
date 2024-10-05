package builderb0y.bigglobe.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.registry.Registry;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.structures.dungeons.AbstractDungeonStructure;
import builderb0y.bigglobe.structures.dungeons.LargeDungeonStructure;
import builderb0y.bigglobe.structures.dungeons.MediumDungeonStructure;
import builderb0y.bigglobe.structures.dungeons.SmallDungeonStructure;
import builderb0y.bigglobe.structures.megaTree.MegaTreeBall;
import builderb0y.bigglobe.structures.megaTree.MegaTreeStructure;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeStructures {

	static { BigGlobeMod.LOGGER.debug("Registering structures..."); }

	//////////////// noop ////////////////

	public static final StructureType<NoopStructure>
		NOOP = registerType("noop", NoopStructure.CODEC);

	//////////////// delegating ////////////////

	public static final StructureType<DelegatingStructure>
		DELEGATING_TYPE = registerType("delegating", DelegatingStructure.CODEC);

	//////////////// common dungeon ////////////////

	public static final BigGlobeStructurePieceType
		DUNGEON_PIT_TYPE = registerPiece("dungeon_pit", AbstractDungeonStructure.PitDungeonPiece::new);

	//////////////// small dungeon ////////////////

	public static final StructureType<SmallDungeonStructure>
		SMALL_DUNGEON_TYPE         = registerType("small_dungeon", SmallDungeonStructure.CODEC);
	public static final BigGlobeStructurePieceType
		SMALL_DUNGEON_ROOM_TYPE    = registerPiece("small_dungeon_room",    SmallDungeonStructure.Room        ::new),
		SMALL_DUNGEON_HALL0_TYPE   = registerPiece("small_dungeon_hall0",   SmallDungeonStructure.Hall0       ::new),
		SMALL_DUNGEON_HALL1_TYPE   = registerPiece("small_dungeon_hall1",   SmallDungeonStructure.Hall1       ::new),
		SMALL_DUNGEON_CHEST_TYPE   = registerPiece("small_dungeon_chest",   SmallDungeonStructure.ChestPiece  ::new),
		SMALL_DUNGEON_SPAWNER_TYPE = registerPiece("small_dungeon_spawner", SmallDungeonStructure.SpawnerPiece::new);

	//////////////// medium dungeon ////////////////

	public static final StructureType<MediumDungeonStructure>
		MEDIUM_DUNGEON_TYPE         = registerType("medium_dungeon", MediumDungeonStructure.CODEC);
	public static final BigGlobeStructurePieceType
		MEDIUM_DUNGEON_ROOM_TYPE    = registerPiece("medium_dungeon_room",    MediumDungeonStructure.Room        ::new),
		MEDIUM_DUNGEON_HALL0_TYPE   = registerPiece("medium_dungeon_hall0",   MediumDungeonStructure.Hall0       ::new),
		MEDIUM_DUNGEON_HALL1_TYPE   = registerPiece("medium_dungeon_hall1",   MediumDungeonStructure.Hall1       ::new),
		MEDIUM_DUNGEON_HALL2_TYPE   = registerPiece("medium_dungeon_hall2",   MediumDungeonStructure.Hall2       ::new),
		MEDIUM_DUNGEON_CHEST_TYPE   = registerPiece("medium_dungeon_chest",   MediumDungeonStructure.ChestPiece  ::new),
		MEDIUM_DUNGEON_SPAWNER_TYPE = registerPiece("medium_dungeon_spawner", MediumDungeonStructure.SpawnerPiece::new);

	//////////////// large dungeon ////////////////

	public static final StructureType<LargeDungeonStructure>
		LARGE_DUNGEON_TYPE = registerType("large_dungeon", LargeDungeonStructure.CODEC);
	public static final BigGlobeStructurePieceType
		LARGE_DUNGEON_ROOM_TYPE    = registerPiece("large_dungeon_room",    LargeDungeonStructure.Room        ::new),
		LARGE_DUNGEON_HALL0_TYPE   = registerPiece("large_dungeon_hall0",   LargeDungeonStructure.Hall0       ::new),
		LARGE_DUNGEON_HALL1_TYPE   = registerPiece("large_dungeon_hall1",   LargeDungeonStructure.Hall1       ::new),
		LARGE_DUNGEON_HALL2_TYPE   = registerPiece("large_dungeon_hall2",   LargeDungeonStructure.Hall2       ::new),
		LARGE_DUNGEON_HALL3_TYPE   = registerPiece("large_dungeon_hall3",   LargeDungeonStructure.Hall3       ::new),
		LARGE_DUNGEON_CHEST_TYPE   = registerPiece("large_dungeon_chest",   LargeDungeonStructure.ChestPiece  ::new),
		LARGE_DUNGEON_SPAWNER_TYPE = registerPiece("large_dungeon_spawner", LargeDungeonStructure.SpawnerPiece::new),
		LARGE_DUNGEON_TRAP_TYPE    = registerPiece("large_dungeon_trap",    LargeDungeonStructure.TrapPiece   ::new);

	//////////////// geode ////////////////

	public static final StructureType<GeodeStructure>
		GEODE_TYPE              = registerType("geode", GeodeStructure.CODEC);
	public static final BigGlobeStructurePieceType
		GEODE_PIECE_TYPE        = registerPiece("geode_piece", GeodeStructure. MainPiece::new),
		GEODE_SPIKE_PIECE_TYPE  = registerPiece("geode_spike", GeodeStructure.SpikePiece::new);

	//////////////// bigger desert pyramid ////////////////

	public static final StructureType<BiggerDesertPyramidStructure>
		BIGGER_DESERT_PYRAMID_TYPE                         = registerType("bigger_desert_pyramid", BiggerDesertPyramidStructure.CODEC);
	public static final BigGlobeStructurePieceType
		BIGGER_DESERT_PYRAMID_PIECE_TYPE                   = registerPiece("bigger_desert_pyramid_piece",                   BiggerDesertPyramidStructure.            MainPiece::new),
		BIGGER_DESERT_PYRAMID_UNDERGROUND_ROOM_PIECE_TYPE  = registerPiece("bigger_desert_pyramid_underground_room_piece",  BiggerDesertPyramidStructure. UndergroundRoomPiece::new),
		BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL0_PIECE_TYPE = registerPiece("bigger_desert_pyramid_underground_hall0_piece", BiggerDesertPyramidStructure.UndergroundHall0Piece::new),
		BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL1_PIECE_TYPE = registerPiece("bigger_desert_pyramid_underground_hall1_piece", BiggerDesertPyramidStructure.UndergroundHall1Piece::new),
		BIGGER_DESERT_PYRAMID_UNDERGROUND_HALL2_PIECE_TYPE = registerPiece("bigger_desert_pyramid_underground_hall2_piece", BiggerDesertPyramidStructure.UndergroundHall2Piece::new);

	//////////////// mega trees ////////////////

	public static final StructureType<MegaTreeStructure>
		MEGA_TREE_TYPE      = registerType("mega_tree", MegaTreeStructure.CODEC);
	public static final BigGlobeStructurePieceType
		MEGA_TREE_BALL_TYPE = registerPiece("mega_tree_ball", MegaTreeBall::new);

	//////////////// portal temple ////////////////

	public static final StructureType<PortalTempleStructure>
		PORTAL_TEMPLE = registerType("portal_temple", PortalTempleStructure.CODEC);
	public static final BigGlobeStructurePieceType
		PORTAL_TEMPLE_MAIN_BUILDING = registerPiece("portal_temple_main_building", PortalTempleStructure.MainBuildingPiece::new),
		PORTAL_TEMPLE_PORTAL        = registerPiece("portal_temple_portal",        PortalTempleStructure.      PortalPiece::new),
		PORTAL_TEMPLE_WELL          = registerPiece("portal_temple_well",          PortalTempleStructure.        WellPiece::new),
		PORTAL_TEMPLE_FARM          = registerPiece("portal_temple_farm",          PortalTempleStructure.        FarmPiece::new),
		PORTAL_TEMPLE_TABLE         = registerPiece("portal_temple_table",         PortalTempleStructure.       TablePiece::new),
		PORTAL_TEMPLE_FURNACE       = registerPiece("portal_temple_furnace",       PortalTempleStructure.     FurnacePiece::new),
		PORTAL_TEMPLE_SPAWNER       = registerPiece("portal_temple_spawner",       PortalTempleStructure.     SpawnerPiece::new);

	//////////////// scripted ////////////////

	public static final StructureType<ScriptedStructure>
		SCRIPTED = registerType("script", ScriptedStructure.CODEC);
	public static final BigGlobeStructurePieceType
		SCRIPTED_PIECE = registerPiece("script_piece", ScriptedStructure.Piece::new);

	//////////////// end ////////////////

	static { BigGlobeMod.LOGGER.debug("Done registering structures."); }

	public static BigGlobeStructurePieceType registerPiece(String name, BigGlobeStructurePieceType type) {
		return Registry.register(RegistryVersions.structurePieceType(), BigGlobeMod.modID(name), type);
	}

	public static <T_Structure extends Structure> StructureType<T_Structure> registerType(String name, #if MC_VERSION >= MC_1_20_5 MapCodec #else Codec #endif <T_Structure> codec) {
		return Registry.register(RegistryVersions.structureType(), BigGlobeMod.modID(name), () -> codec);
	}

	public static <T_Placement extends StructurePlacement> StructurePlacementType<T_Placement> registerPlacement(String name, #if MC_VERSION >= MC_1_20_5 MapCodec #else Codec #endif <T_Placement> codec) {
		return Registry.register(RegistryVersions.structurePlacementType(), BigGlobeMod.modID(name), () -> codec);
	}

	public static void init() {}
}