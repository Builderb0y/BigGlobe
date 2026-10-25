package builderb0y.bigglobe.platform.fabric.fabric.api.client.particle.v1;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public interface ParticleFactoryRegistry {

	static ParticleFactoryRegistry getInstance() {
		return Holder.INSTANCE;
	}

	<T extends ParticleOptions> void register(ParticleType<T> type, PendingParticleFactory<T> constructor);

	@FunctionalInterface
	interface PendingParticleFactory<T extends ParticleOptions> {
		ParticleProvider<T> create(SpriteSet sprites);
	}

	class Holder {
		public static final Map<ParticleType<?>, PendingParticleFactory<?>> FACTORIES = new HashMap<>();

		public static final ParticleFactoryRegistry INSTANCE = new ParticleFactoryRegistry() {
			@Override
			public <T extends ParticleOptions> void register(ParticleType<T> type, PendingParticleFactory<T> constructor) {
				FACTORIES.put(type, constructor);
			}
		};
	}
}
