package builderb0y.bigglobe.compat;

import java.util.function.BiConsumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import builderb0y.bigglobe.ClientState;
import builderb0y.bigglobe.mixinInterfaces.LodSystemHolder;
import builderb0y.bigglobe.versions.EntityVersions;

public class ImmersivePortalsCompat {

	public static void init() {
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
	}

	public static void forEachDimension(MinecraftServer server, ServerPlayer player, BiConsumer<ServerLevel, ServerPlayer> action) {
		action.accept(EntityVersions.getServerWorld(player), player);
	}

	@Environment(EnvType.CLIENT)
	public static @Nullable LodSystemHolder getLodSystem(ResourceKey<Level> dimensionKey) {
		ClientLevel world = Minecraft.getInstance().level;
		if (world != null && world.dimension() == dimensionKey) {
			return LodSystemHolder.of(Minecraft.getInstance().levelRenderer);
		}
		return null;
	}
}
