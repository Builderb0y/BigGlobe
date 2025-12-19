package builderb0y.bigglobe.mixins;

#if MC_VERSION >= MC_1_21_11

	import com.llamalad7.mixinextras.injector.ModifyReceiver;
	import org.spongepowered.asm.mixin.Mixin;
	import org.spongepowered.asm.mixin.injection.At;

	import net.minecraft.block.entity.CreakingHeartBlockEntity;
	import net.minecraft.server.world.ServerWorld;
	import net.minecraft.world.World;

	import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;

	@Mixin(CreakingHeartBlockEntity.class)
	public class CreakingHeartBlockEntity_MakeWorkInTheNether {

		@ModifyReceiver(method = "getBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEnvironmentAttributes()Lnet/minecraft/world/attribute/WorldEnvironmentAttributeAccess;"))
		private static World bigglobe_makeWorkInNether(World world) {
			if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.creaking_overrides != null) {
				ServerWorld replacement = serverWorld.getServer().getWorld(generator.creaking_overrides.time_reference());
				if (replacement != null) return replacement;
			}
			return world;
		}

		@ModifyReceiver(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEnvironmentAttributes()Lnet/minecraft/world/attribute/WorldEnvironmentAttributeAccess;"))
		private static World bigglobe_dontKillInNether(World world) {
			if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator && generator.creaking_overrides != null) {
				ServerWorld replacement = serverWorld.getServer().getWorld(generator.creaking_overrides.time_reference());
				if (replacement != null) return replacement;
			}
			return world;
		}
	}

#endif