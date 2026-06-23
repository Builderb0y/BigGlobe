package builderb0y.bigglobe.blockdefs;

import java.util.Collections;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.registry.LandPathTypeRegistry;

import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.DelayedGenerationBlock;
import builderb0y.bigglobe.blocks.MoltenRockBlock;
import builderb0y.bigglobe.blocks.RopeAnchorBlock;
import builderb0y.bigglobe.blocks.SpelunkingRopeBlock;

public class BigGlobeBlocks {

	static {
		BigGlobeMod.LOGGER.debug("Registering blocks...");
	}

	public static final SpelunkingRopeBlock SPELUNKING_ROPE = register(
		"spelunking_rope",
		new SpelunkingRopeBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(key("spelunking_rope"))
			.mapColor(MapColor.WOOD)
			.strength(0.8f)
			.sound(SoundType.WOOL)
			.pushReaction(PushReaction.DESTROY)
		)
	);
	public static final RopeAnchorBlock ROPE_ANCHOR = register(
		"rope_anchor",
		new RopeAnchorBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(key("rope_anchor"))
			.mapColor(MapColor.METAL)
			.requiresCorrectToolForDrops()
			.strength(5.0F)
			.sound(SoundType.DEEPSLATE_BRICKS)
			.pushReaction(PushReaction.BLOCK)
		)
	);

	public static final DelayedGenerationBlock DELAYED_GENERATION = register(
		"delayed_generation",
		new DelayedGenerationBlock(
			BlockBehaviour
			.Properties
			.of()
			.setId(key("delayed_generation"))
			.mapColor(MapColor.NONE)
			.instabreak()
			.noCollision()
			.noOcclusion()
			.noLootTable()
			.pushReaction(PushReaction.BLOCK)
		)
	);

	static {
		BigGlobeMod.LOGGER.debug("Done registering blocks.");
	}

	public static FlowerPotBlock newPottedPlant(Block plant, String key) {
		int lightLevel = plant.defaultBlockState().getLightEmission();
		return new FlowerPotBlock(
			plant,
			BlockBehaviour
			.Properties
			.of()
			.setId(key(key))
			.mapColor(plant.defaultMapColor())
			.instabreak()
			.noOcclusion()
			.lightLevel((BlockState state) -> lightLevel)
			.pushReaction(PushReaction.DESTROY)
		);
	}

	public static <B extends Block> B register(String name, B block) {
		Identifier id = BigGlobeMod.modID(name);
		if (!block.getDescriptionId().equals(Util.makeDescriptionId("block", id))) {
			throw new IllegalArgumentException("Name mismatch");
		}
		return Registry.register(BuiltInRegistries.BLOCK, id, block);
	}

	public static ResourceKey<Block> key(String name) {
		return ResourceKey.create(Registries.BLOCK, BigGlobeMod.modID(name));
	}

	public static void init() {
		OverworldBlocks.init();
		NetherBlocks.init();
		EndBlocks.init();
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		OverworldBlocks.initClient();
		NetherBlocks.initClient();
		EndBlocks.initClient();
	}
}