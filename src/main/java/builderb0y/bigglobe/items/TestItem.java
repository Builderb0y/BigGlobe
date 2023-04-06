package builderb0y.bigglobe.items;

import com.google.common.base.Predicates;
import org.jetbrains.annotations.TestOnly;

import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import builderb0y.bigglobe.BigGlobeMod;

@TestOnly
@Deprecated
public class TestItem extends Item {

	public TestItem() {
		super(new Item.Settings());
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (!(context.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;

		this.placeStructure(
			world,
			context.getBlockPos(),
			world
			.getRegistryManager()
			.get(RegistryKeys.STRUCTURE)
			.get(BigGlobeMod.modID("dungeons/large"))
		);
		return ActionResult.SUCCESS;
	}

	public void placeStructure(ServerWorld world, BlockPos pos, Structure structure) {
		System.out.println("spawning structure");
		ChunkPos chunkPos = new ChunkPos(pos);
		StructurePosition position = (
			structure.getValidStructurePosition(
				new Structure.Context(
					world.getRegistryManager(),
					world.getChunkManager().getChunkGenerator(),
					world.getChunkManager().getChunkGenerator().getBiomeSource(),
					world.getChunkManager().getNoiseConfig(),
					world.getStructureTemplateManager(),
					world.getSeed(),
					chunkPos,
					world,
					Predicates.alwaysTrue()
				)
			)
			.orElse(null)
		);
		if (position != null) {
			StructurePiecesList pieces = position.generate().toList();
			BlockBox firstBox = pieces.pieces().get(0).getBoundingBox();
			BlockPos pivot = firstBox.getCenter();
			pivot = new BlockPos(pivot.getX(), firstBox.getMinY(), pivot.getZ());
			int offset = pos.getY() - pieces.getBoundingBox().getMinY() - 1;
			BlockBox infiniteBox = BlockBox.infinite();
			for (StructurePiece piece : pieces.pieces()) {
				piece.translate(0, offset, 0);
				piece.generate(
					world,
					world.getStructureAccessor(),
					world.getChunkManager().getChunkGenerator(),
					world.random,
					infiniteBox,
					chunkPos,
					pivot
				);
			}
		}
		else {
			System.out.println("Structure didn't want to spawn.");
		}
	}
}