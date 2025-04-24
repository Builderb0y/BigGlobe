package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.UseSuperClass;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION >= MC_1_20_5
	import net.minecraft.particle.SimpleParticleType;
#else
	import net.minecraft.particle.DefaultParticleType;
#endif

public class BlazingBlossomBlock extends NetherFlowerBlock {

	#if MC_VERSION >= MC_1_20_3

		public static final MapCodec<BlazingBlossomBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BlazingBlossomBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}

	#endif

	public final RegistryEntry<ParticleType<?>> particle;

	#if MC_VERSION >= MC_1_20_5

		public BlazingBlossomBlock(RegistryEntry<StatusEffect> suspicious_stew_effect, float effect_duration, RegistryEntry<ParticleType<?>> particle, Settings settings) {
			super(suspicious_stew_effect, effect_duration, settings);
			this.particle = particle;
		}

	#else

		public BlazingBlossomBlock(RegistryEntry<StatusEffect> suspicious_stew_effect, int effect_duration, RegistryEntry<ParticleType<?>> particle, Settings settings) {
			super(suspicious_stew_effect, effect_duration, settings);
			this.particle = particle;
		}

	#endif

	public static RegistryEntry<ParticleType<?>> particleEntry(#if MC_VERSION >= MC_1_20_5 SimpleParticleType #else DefaultParticleType #endif type) {
		return RegistryVersions.getEntry(Registries.PARTICLE_TYPE, type);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity #if MC_VERSION >= MC_1_21_5 , net.minecraft.entity.EntityCollisionHandler handler #endif) {
		super.onEntityCollision(state, world, pos, entity #if MC_VERSION >= MC_1_21_5 , handler #endif);
		if (entity.getType().isFireImmune() || (entity instanceof PlayerEntity player && player.isCreative())) {
			return;
		}
		switch (world.getDifficulty()) {
			case PEACEFUL -> {}
			case EASY     -> entity.setOnFireFor(2);
			case NORMAL   -> entity.setOnFireFor(4);
			case HARD     -> entity.setOnFireFor(6);
		}
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		super.randomDisplayTick(state, world, pos, random);
		if (this.particle.value() instanceof ParticleEffect particle) {
			if (random.nextBoolean()) return;
			Vec3d offset = state.getModelOffset(#if MC_VERSION < MC_1_21_2 world, #endif pos);
			double motionX, motionZ;
			Permuter permuter = Permuter.from(random);
			do {
				motionX = Permuter.nextUniformDouble(permuter);
				motionZ = Permuter.nextUniformDouble(permuter);
			}
			while (BigGlobeMath.squareD(motionX, motionZ) > 1.0D);
			world. #if MC_VERSION >= MC_1_21_5 addParticleClient #else addParticle #endif (
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