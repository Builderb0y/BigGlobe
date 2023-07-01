package builderb0y.bigglobe.blockEntities;

import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BlockEntityType.BlockEntityFactory;
import net.minecraft.util.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BigGlobeBlockEntityTypes {

	static { BigGlobeMod.LOGGER.debug("Registering block entity types..."); }

	public static final BlockEntityType<DelayedGenerationBlockEntity> DELAYED_GENERATION = register("delayed_generation", (pos, state) -> new DelayedGenerationBlockEntity(BigGlobeBlockEntityTypes.DELAYED_GENERATION, pos, state), BigGlobeBlocks.DELAYED_GENERATION);

	static { BigGlobeMod.LOGGER.debug("Done registering block entity types."); }

	public static void init() {} //triggers static class initializers.

	public static <B extends BlockEntity> BlockEntityType<B> register(String name, BlockEntityFactory<B> factory, Block... blocks) {
		return Registry.register(RegistryVersions.blockEntityType(), BigGlobeMod.modID(name), new BlockEntityType<>(factory, Set.of(blocks), null));
	}
}