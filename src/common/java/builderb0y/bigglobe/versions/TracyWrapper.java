package builderb0y.bigglobe.versions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.BigGlobeMod;

public class TracyWrapper {

	public static final boolean ENABLED;
	public static final DecimalFormat FORMAT;

	static {
		boolean tracy = Boolean.getBoolean(BigGlobeMod.MODID + ".tracy");
		/*
		if (tracy) try {
			TracyClient.load();
			BigGlobeMod.LOGGER.info("Tracy requested and available.");
		}
		catch (Throwable error) {
			BigGlobeMod.LOGGER.error("Tracy requested, but unavailable:", error);
			tracy = false;
		}
		*/
		ENABLED = tracy;
		if (tracy) {
			FORMAT = new DecimalFormat();
			FORMAT.setMinimumIntegerDigits(2);
			FORMAT.setMinimumFractionDigits(2);
		}
		else {
			FORMAT = null;
		}
	}

	public static ZoneWrapper current;

	public static @Nullable ZoneWrapper beginZone(String name) {
		return beginZone(() -> name);
	}

	public static @Nullable ZoneWrapper beginZone(Supplier<String> name) {
		return ENABLED ? current = new ZoneWrapper(current, name) : null;
	}

	public static void endZone(@Nullable ZoneWrapper zone) {
		if (zone != null) {
			zone.endTime = System.nanoTime();
			zone.completed = true;
			if ((current = zone.parent) == null) {
				long time = zone.endTime - zone.startTime;
				dumpZone(zone, 0, time, time);
			}
		}
	}

	public static void dumpZone(ZoneWrapper zone, int indentation, long parentTime, long totalTime) {
		String name = zone.nameSupplier.get();
		String indentString = "\t".repeat(indentation++);
		System.out.println(indentString + "BEGIN " + name + ": " + zone.startTime);
		long selfTime = zone.endTime - zone.startTime;
		if (zone.children != null) {
			for (ZoneWrapper child : zone.children) {
				dumpZone(child, indentation, selfTime, totalTime);
			}
		}
		if (zone.completed) {
			System.out.println(indentString + "END " + name + ": " + selfTime + " / " + totalTime + " (" + FORMAT.format((((double)(selfTime)) / ((double)(parentTime))) * 100.0D) + "% parent, " + FORMAT.format((((double)(selfTime)) / ((double)(totalTime))) * 100.0D) + "% total)");
		}
		else {
			System.out.println(indentString + "END " + name);
		}
	}

	public static class ZoneWrapper implements AutoCloseable {

		public ZoneWrapper parent;
		public @Nullable List<ZoneWrapper> children;
		public Supplier<String> nameSupplier;
		public long startTime;
		public long endTime;
		public boolean completed;

		public ZoneWrapper(ZoneWrapper parent, Supplier<String> nameSupplier) {
			this.startTime = System.nanoTime();
			this.parent = parent;
			this.nameSupplier = nameSupplier;
			if (parent != null) {
				if (parent.children == null) {
					parent.children = new ArrayList<>();
				}
				parent.children.add(this);
			}
		}

		@Override
		public void close() {
			endZone(this);
		}
	}
}