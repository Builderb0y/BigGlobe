package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.ArrayList;
import java.util.List;

public class WorldRenderEvents {

	@FunctionalInterface
	public interface AfterSetup {
		void afterSetup(WorldRenderContext context);
	}

	@FunctionalInterface
	public interface StartMain {
		void onStart(WorldRenderContext context);
	}

	@FunctionalInterface
	public interface EndExtraction {
		void onEndExtraction(WorldRenderContext context);
	}

	public static final Event<AfterSetup> AFTER_SETUP = new Event<>();
	public static final Event<StartMain> START_MAIN = new Event<>();
	public static final Event<EndExtraction> END_EXTRACTION = new Event<>();

	public static class Event<T> {
		private final List<T> listeners = new ArrayList<>();

		public void register(T listener) {
			this.listeners.add(listener);
		}

		public List<T> getListeners() {
			return this.listeners;
		}
	}
}
