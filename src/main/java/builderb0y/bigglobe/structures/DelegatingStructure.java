package builderb0y.bigglobe.structures;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class DelegatingStructure extends BigGlobeStructure {

	public static final Codec<DelegatingStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(DelegatingStructure.class);

	public final RegistryEntry<Structure> delegate;

	public DelegatingStructure(Config config, RegistryEntry<Structure> delegate) {
		super(config);
		this.delegate = delegate;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		return this.delegate.value().getStructurePosition(context);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.DELEGATING_TYPE;
	}
}