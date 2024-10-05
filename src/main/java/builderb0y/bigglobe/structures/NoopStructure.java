package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class NoopStructure extends BigGlobeStructure {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<NoopStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NoopStructure.class);
	#else
		public static final Codec<NoopStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NoopStructure.class).codec();
	#endif

	public NoopStructure(Config config) {
		super(config, null);
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		return Optional.empty();
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.NOOP;
	}
}