package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.server.MinecraftServer;

/**
this mixin changes the number of spawn chunks
minecraft will force-load when creating a world.
since I generate a lot of worlds from scratch in development,
it helps to have this process go as quickly as possible,
which means pre-generating fewer chunks.

a mod named fastload used to serve this purpose quite well,
but with more recent versions came more bugs,
and in general I'd rather not have to deal with fastload anymore.

this mixin is disabled by default and must be manually
enabled in config/bigglobe/mixins.properties.
note that fastload modifies the same constant I'm modifying here,
so things will most likely not work correctly if
fastload is installed while this mixin is enabled.
*/
@Mixin(MinecraftServer.class)
public class MinecraftServer_LoadSmallerSpawnArea {

	@ModifyConstant(method = "prepareStartRegion", constant = @Constant(intValue = 11))
	private int bigglobe_overrideRadius(int oldValue) {
		return 1;
	}

	@ModifyConstant(method = "prepareStartRegion", constant = @Constant(intValue = 441))
	private int bigglobe_overrideTotalChunks(int oldValue) {
		return 1;
	}
}