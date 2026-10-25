package builderb0y.bigglobe.platform.fabric.loader.api;

public interface Version extends Comparable<Version> {

	String getFriendlyString();

	static Version parse(String string) throws VersionParsingException {
		return new Impl(string);
	}

	class Impl implements Version {
		private final String string;

		public Impl(String string) {
			this.string = string;
		}

		@Override
		public String getFriendlyString() {
			return this.string;
		}

		@Override
		public int compareTo(Version o) {
			return this.string.compareTo(o.getFriendlyString());
		}

		@Override
		public String toString() {
			return this.string;
		}
	}
}
