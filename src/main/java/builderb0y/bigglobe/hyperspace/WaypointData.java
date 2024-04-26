package builderb0y.bigglobe.hyperspace;

import java.util.UUID;

import it.unimi.dsi.fastutil.Hash;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import builderb0y.autocodec.util.HashStrategies;

public interface WaypointData {

	public static final Hash.Strategy<WaypointData> UUID_STRATEGY = HashStrategies.map(HashStrategies.defaultStrategy(), WaypointData::uuid);

	public abstract UUID uuid();

	public abstract UUID owner();

	public abstract RegistryKey<World> world();
}