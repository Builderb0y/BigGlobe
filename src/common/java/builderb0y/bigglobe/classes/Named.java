package builderb0y.bigglobe.classes;

import it.unimi.dsi.fastutil.Hash;

import builderb0y.autocodec.util.HashStrategies;

public interface Named {

	public static final Hash.Strategy<Named>
		NAME_STRATEGY = HashStrategies.map(HashStrategies.defaultStrategy(), Named::name);

	public abstract String name();
}