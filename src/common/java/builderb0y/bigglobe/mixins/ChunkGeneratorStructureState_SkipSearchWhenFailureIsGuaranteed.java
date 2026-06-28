package builderb0y.bigglobe.mixins;

import java.util.function.Predicate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

@Mixin(ChunkGeneratorStructureState.class)
public class ChunkGeneratorStructureState_SkipSearchWhenFailureIsGuaranteed {

	@WrapOperation(method = "lambda$generateRingPositions$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/BiomeSource;findBiomeHorizontal(IIIILjava/util/function/Predicate;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/biome/Climate$Sampler;)Lcom/mojang/datafixers/util/Pair;"))
	private @Nullable Pair<BlockPos, Holder<Biome>> bigglobe_skipSearchWhenFailureIsGuaranteed(
		BiomeSource instance,
		int x,
		int y,
		int z,
		int searchRadius,
		Predicate<Holder<Biome>> allowed,
		RandomSource random,
		Sampler sampler,
		Operation<Pair<BlockPos, Holder<Biome>>> original
	) {
		for (Holder<Biome> biome : instance.possibleBiomes()) {
			if (allowed.test(biome)) {
				return original.call(instance, x, y, z, searchRadius, allowed, random, sampler);
			}
		}
		return null;
	}
}