package builderb0y.bigglobe.mixinInterfaces;

import builderb0y.bigglobe.hyperspace.PlayerWaypointManager;

public interface WaypointTracker {

	public abstract PlayerWaypointManager bigglobe_getWaypointManager();

	public abstract void bigglobe_setWaypointManager(PlayerWaypointManager data);
}