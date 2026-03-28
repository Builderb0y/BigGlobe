package builderb0y.bigglobe.items;

import com.google.common.base.Predicates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.jetbrains.annotations.TestOnly;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.RegistryVersions;

@TestOnly
@Deprecated
public class TestItem extends Item {

	public TestItem() {
		super(new Item.Properties());
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel world)) return InteractionResult.SUCCESS;

		this.placeStructure(
			world,
			context.getClickedPos(),
			RegistryVersions.getRegistry(
					world.registryAccess(),
					Registries.STRUCTURE
				)
				.getValue(BigGlobeMod.modID("dungeons/large"))
		);
		return InteractionResult.SUCCESS;
	}

	public void placeStructure(ServerLevel world, BlockPos pos, Structure structure) {
		System.out.println("spawning structure");
		ChunkPos chunkPos = new ChunkPos(pos);
		GenerationStub position = (
			structure.findValidGenerationPoint(
					new Structure.GenerationContext(
						world.registryAccess(),
						world.getChunkSource().getGenerator(),
						world.getChunkSource().getGenerator().getBiomeSource(),
						world.getChunkSource().randomState(),
						world.getStructureManager(),
						world.getSeed(),
						chunkPos,
						world,
						Predicates.alwaysTrue()
					)
				)
				.orElse(null)
		);
		if (position != null) {
			PiecesContainer pieces = position.getPiecesBuilder().build();
			BoundingBox firstBox = pieces.pieces().get(0).getBoundingBox();
			BlockPos pivot = firstBox.getCenter();
			pivot = new BlockPos(pivot.getX(), firstBox.minY(), pivot.getZ());
			int offset = pos.getY() - pieces.calculateBoundingBox().minY() - 1;
			BoundingBox infiniteBox = BoundingBox.infinite();
			for (StructurePiece piece : pieces.pieces()) {
				piece.move(0, offset, 0);
				piece.postProcess(
					world,
					world.structureManager(),
					world.getChunkSource().getGenerator(),
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