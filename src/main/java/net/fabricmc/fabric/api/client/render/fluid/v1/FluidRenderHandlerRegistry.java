package net.fabricmc.fabric.api.client.render.fluid.v1;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.material.Fluid;

public class FluidRenderHandlerRegistry {

	public static final FluidRenderHandlerRegistry INSTANCE = new FluidRenderHandlerRegistry();

	private final Map<Fluid, FluidRenderHandler> handlers = new HashMap<>();

	public void register(Fluid still, Fluid flowing, FluidRenderHandler handler) {
		this.handlers.put(still, handler);
		this.handlers.put(flowing, handler);
	}

	public FluidRenderHandler get(Fluid fluid) {
		return this.handlers.get(fluid);
	}
}
