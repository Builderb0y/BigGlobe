package builderb0y.bigglobe.blockdefs;

import net.fabricmc.fabric.api.registry.LandPathTypeRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;

import builderb0y.bigglobe.blocks.*;
import builderb0y.bigglobe.brewing.BigGlobeBrewing;
import builderb0y.bigglobe.fluids.BigGlobeFluids;

public class NetherBlocks {

	public static void init() {
		CharredBlocks.init();
		LandPathTypeRegistry.register(BLAZING_BLOSSOM, PathType.FIRE, PathType.FIRE_IN_NEIGHBOR);
		LandPathTypeRegistry.register(SOUl_MAGMA, PathType.FIRE, PathType.FIRE_IN_NEIGHBOR);
	}

	public static void initClient() {
		CharredBlocks.initClient();
	}

	public static final AshenNetherrackBlock ASHEN_NETHERRACK = BigGlobeBlocks.register(
		new AshenNetherrackBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("ashen_netherrack"))
			.mapColor(MapColor.COLOR_BLACK)
			.requiresCorrectToolForDrops()
			.strength(0.4F)
			.sound(SoundType.NETHERRACK)
		)
	);
	public static final Block SULFUR_ORE = BigGlobeBlocks.register(
		new DropExperienceBlock(
			UniformInt.of(0, 2),
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("sulfur_ore"))
			.mapColor(MapColor.NETHER)
			.strength(3.0F)
			.requiresCorrectToolForDrops()
			.sound(SoundType.NETHER_ORE)
		)
	);
	public static final Block SULFUR_BLOCK = BigGlobeBlocks.register(
		new Block(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("sulfur_block"))
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(5.0F, 6.0F)
			.requiresCorrectToolForDrops()
		)
	);
	public static final NetherGrassBlock WART_WEED = BigGlobeBlocks.register(
		new NetherGrassBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("wart_weed"))
			.mapColor(MapColor.COLOR_RED)
			.noOcclusion()
			.noCollision()
			.instabreak()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final NetherGrassBlock CHARRED_GRASS = BigGlobeBlocks.register(
		new NetherGrassBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("charred_grass"))
			.mapColor(MapColor.COLOR_BLACK)
			.noOcclusion()
			.noCollision()
			.instabreak()
			.sound(SoundType.GRASS)
			.offsetType(OffsetType.XZ)
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final BlazingBlossomBlock BLAZING_BLOSSOM = BigGlobeBlocks.register(
		new BlazingBlossomBlock(
			MobEffects.FIRE_RESISTANCE,
			8,
			BlazingBlossomBlock.particleEntry(ParticleTypes.FLAME),
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("blazing_blossom"))
			.mapColor(MapColor.TERRACOTTA_ORANGE)
			.instabreak()
			.noOcclusion()
			.noCollision()
			.sound(SoundType.GRASS)
			.lightLevel((BlockState state) -> 7)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_BLAZING_BLOSSOM = BigGlobeBlocks.register(
		BigGlobeBlocks.newPottedPlant(BLAZING_BLOSSOM, "potted_blazing_blossom")
	);
	public static final BlazingBlossomBlock SOUL_SILVERPETAL = BigGlobeBlocks.register(
		new BlazingBlossomBlock(
			BigGlobeBrewing.SOUL_SIPHON,
			8,
			BlazingBlossomBlock.particleEntry(ParticleTypes.SOUL_FIRE_FLAME),
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("soul_silverpetal"))
			.mapColor(MapColor.DIAMOND)
			.instabreak()
			.noOcclusion()
			.noCollision()
			.sound(SoundType.GRASS)
			.lightLevel((BlockState state) -> 5)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final NetherFlowerBlock GLOWING_GOLDENROD = BigGlobeBlocks.register(
		new NetherFlowerBlock(
			MobEffects.GLOWING,
			8,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("glowing_goldenrod"))
			.mapColor(MapColor.SAND)
			.instabreak()
			.noOcclusion()
			.noCollision()
			.sound(SoundType.GRASS)
			.lightLevel((BlockState state) -> 11)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final FlowerPotBlock POTTED_GLOWING_GOLDENROD = BigGlobeBlocks.register(
		BigGlobeBlocks.newPottedPlant(GLOWING_GOLDENROD, "potted_glowing_goldenrod")
	);
	public static final SoulLavaBlock SOUL_LAVA = BigGlobeBlocks.register(
		new SoulLavaBlock(
			BigGlobeFluids.SOUL_LAVA.builtInRegistryHolder(),
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("soul_lava"))
			.mapColor(MapColor.DIAMOND)
			.noCollision()
			.randomTicks()
			.strength(100.0F)
			.lightLevel((BlockState state) -> 15)
			.noLootTable()
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final MagmaBlock SOUl_MAGMA = BigGlobeBlocks.register(
		new MagmaBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.MAGMA_BLOCK)
			.setId(BigGlobeBlocks.key("soul_magma"))
			.mapColor(MapColor.LAPIS)
			.isValidSpawn((BlockState state, BlockGetter world, BlockPos pos, EntityType<?> type) -> type.fireImmune()) //not copied by copy().
		)
	);
	public static final SoulCauldronBlock SOUL_CAULDRON = BigGlobeBlocks.register(
		new SoulCauldronBlock(
			BlockBehaviour
			.Properties
			.ofFullCopy(Blocks.LAVA_CAULDRON)
			.setId(BigGlobeBlocks.key("soul_cauldron"))
		)
	);
	public static final HiddenLavaBlock HIDDEN_LAVA = BigGlobeBlocks.register(
		new HiddenLavaBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("hidden_lava"))
			.mapColor(MapColor.FIRE)
			.noLootTable()
			.pushReaction(PushReaction.DESTROY)
			.replaceable()
		)
	);
	public static final Block ROUGH_QUARTZ = BigGlobeBlocks.register(
		new AmethystBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("rough_quartz"))
			.mapColor(MapColor.QUARTZ)
			.strength(1.5F)
			.sound(SoundType.AMETHYST)
			.requiresCorrectToolForDrops()
		)
	);
	public static final Block BUDDING_QUARTZ = BigGlobeBlocks.register(
		new BuddingQuartzBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("budding_quartz"))
			.mapColor(MapColor.QUARTZ)
			.randomTicks()
			.strength(1.5F)
			.sound(SoundType.AMETHYST)
			.requiresCorrectToolForDrops()
		)
	);
	public static final Block QUARTZ_CLUSTER = BigGlobeBlocks.register(
		new AmethystClusterBlock(
			7,
			3,
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("quartz_cluster"))
			.mapColor(MapColor.QUARTZ)
			.noOcclusion()
			.randomTicks()
			.sound(SoundType.AMETHYST_CLUSTER)
			.strength(1.5F)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block SMALL_QUARTZ_BUD = BigGlobeBlocks.register(
		new AmethystClusterBlock(
			3,
			4,
			BlockBehaviour
			.Properties
			.ofFullCopy(QUARTZ_CLUSTER)
			.setId(BigGlobeBlocks.key("small_quartz_bud"))
			.sound(SoundType.SMALL_AMETHYST_BUD)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block MEDIUM_QUARTZ_BUD = BigGlobeBlocks.register(
		new AmethystClusterBlock(
			4,
			3,
			BlockBehaviour
			.Properties
			.ofFullCopy(QUARTZ_CLUSTER)
			.setId(BigGlobeBlocks.key("medium_quartz_bud"))
			.sound(SoundType.LARGE_AMETHYST_BUD)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block LARGE_QUARTZ_BUD = BigGlobeBlocks.register(
		new AmethystClusterBlock(
			5,
			3,
			BlockBehaviour
			.Properties
			.ofFullCopy(QUARTZ_CLUSTER)
			.setId(BigGlobeBlocks.key("large_quartz_bud"))
			.sound(SoundType.MEDIUM_AMETHYST_BUD)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final Block PALE_NETHERRACK = BigGlobeBlocks.register(
		new Block(
			BlockBehaviour
			.Properties
			.of()
			.setId(BigGlobeBlocks.key("pale_netherrack"))
			.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.requiresCorrectToolForDrops()
			.strength(0.4F)
			.sound(SoundType.NETHERRACK)
		)
	);
}