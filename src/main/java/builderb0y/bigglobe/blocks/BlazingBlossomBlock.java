package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;

public class BlazingBlossomBlock extends NetherFlowerBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<BlazingBlossomBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(BlazingBlossomBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	public BlazingBlossomBlock(#if MC_VERSION >= MC_1_20_5 RegistryEntry<StatusEffect> #else StatusEffect #endif suspiciousStewEffect, int effectDuration, Settings settings) {
		super(suspiciousStewEffect, effectDuration, settings);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		super.onEntityCollision(state, world, pos, entity);
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
		if (random.nextBoolean()) return;
		Vec3d offset = state.getModelOffset(world, pos);
		double motionX, motionZ;
		Permuter permuter = Permuter.from(random);
		do {
			motionX = Permuter.nextUniformDouble(permuter);
			motionZ = Permuter.nextUniformDouble(permuter);
		}
		while (BigGlobeMath.squareD(motionX, motionZ) > 1.0D);
		world.addParticle(
			ParticleTypes.FLAME,
			pos.getX() + 0.5D + offset.x,
			pos.getY() + 0.76D + offset.y,
			pos.getZ() + 0.5D + offset.z,
			motionX * (1.0D / 256.0D),
			permuter.nextDouble() * (1.0D / 64.0D),
			motionZ * (1.0D / 256.0D)
		);
	}
}