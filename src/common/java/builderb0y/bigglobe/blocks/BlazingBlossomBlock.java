package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BlazingBlossomBlock extends NetherFlowerBlock {

	public static final MapCodec<BlazingBlossomBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BlazingBlossomBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public final Holder<ParticleType<?>> particle;

	public BlazingBlossomBlock(Holder<MobEffect> suspicious_stew_effect, float effect_duration, Holder<ParticleType<?>> particle, Properties settings) {
		super(suspicious_stew_effect, effect_duration, settings);
		this.particle = particle;
	}

	public static Holder<ParticleType<?>> particleEntry(SimpleParticleType type) {
		return RegistryVersions.getEntry(BuiltInRegistries.PARTICLE_TYPE, type);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void entityInside(
		BlockState state,
		Level world,
		BlockPos pos,
		Entity entity
		, InsideBlockEffectApplier handler
		, boolean movingFastOrBlockPosIsInsideDestinationBox
	) {
		super.entityInside(
			state,
			world,
			pos,
			entity
			, handler
			, movingFastOrBlockPosIsInsideDestinationBox
		);
		if (entity.getType().fireImmune() || (entity instanceof Player player && player.isCreative())) {
			return;
		}
		switch (world.getDifficulty()) {
			case PEACEFUL -> {
			}
			case EASY -> entity.igniteForSeconds(2);
			case NORMAL -> entity.igniteForSeconds(4);
			case HARD -> entity.igniteForSeconds(6);
		}
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		super.animateTick(state, world, pos, random);
		if (this.particle.value() instanceof ParticleOptions particle) {
			if (random.nextBoolean()) return;
			Vec3 offset = state.getOffset(pos);
			double motionX, motionZ;
			Permuter permuter = Permuter.from(random);
			do {
				motionX = Permuter.nextUniformDouble(permuter);
				motionZ = Permuter.nextUniformDouble(permuter);
			}
			while (BigGlobeMath.squareD(motionX, motionZ) > 1.0D);
			world.addParticle(
				particle,
				pos.getX() + 0.5D + offset.x,
				pos.getY() + 0.75D + offset.y,
				pos.getZ() + 0.5D + offset.z,
				motionX * (1.0D / 256.0D),
				permuter.nextDouble() * (1.0D / 64.0D),
				motionZ * (1.0D / 256.0D)
			);
		}
	}
}