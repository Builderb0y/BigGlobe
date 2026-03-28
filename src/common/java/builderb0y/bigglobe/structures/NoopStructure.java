package builderb0y.bigglobe.structures;

import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.StructureType;
import com.mojang.serialization.MapCodec;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class NoopStructure extends BigGlobeStructure {

	public static final MapCodec<NoopStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NoopStructure.class);

	public NoopStructure(StructureSettings config) {
		super(config, null, null);
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return 0;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return Optional.empty();
	}

	@Override
	public StructureType<?> type() {
		return BigGlobeStructures.NOOP;
	}
}