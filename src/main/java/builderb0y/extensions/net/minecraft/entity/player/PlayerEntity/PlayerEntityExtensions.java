package builderb0y.extensions.net.minecraft.entity.player.PlayerEntity;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;
import builderb0y.bigglobe.mixinInterfaces.WaypointTracker;

@Extension
public class PlayerEntityExtensions {

	public static @Nullable PlayerWaypointManager getWaypointManager(@This PlayerEntity player) {
		return ((WaypointTracker)(player)).bigglobe_getWaypointManager();
	}
}