package builderb0y.bigglobe.generators;

import java.util.HashMap;
import java.util.Map;

public class Table {

	public static record Key(int x, int y) {}
	public final Map<Key, Cell> table = new HashMap<>();
	public int maxX = -1, maxY = -1;

	public Cell getCell(int x, int y) {
		this.maxX = Math.max(this.maxX, x);
		this.maxY = Math.max(this.maxY, y);
		return this.table.computeIfAbsent(new Key(x, y), (Key key) -> new Cell());
	}

	public StringBuilder toStringBuilder() {
		StringBuilder result = new StringBuilder();
		if (this.maxX >= 0 && this.maxY >= 0) {
			int[] maxWidths = new int[this.maxX + 1];
			for (int x = 0; x <= this.maxX; x++) {
				for (int y = 0; y <= this.maxY; y++) {
					Cell cell = this.table.get(new Key(x, y));
					if (cell != null) {
						maxWidths[x] = Math.max(maxWidths[x], cell.length());
					}
				}
			}
			for (int y = 0; y <= this.maxY; y++) {
				for (int x = 0; x <= this.maxX; x++) {
					int spaces = maxWidths[x];
					Cell cell = this.table.get(new Key(x, y));
					if (cell != null) {
						spaces -= cell.length();
						switch (cell.justification) {
							case LEFT -> {
								result.append(cell.builder);
								if (x < this.maxX) result.repeat(" ", spaces);
							}
							case RIGHT -> {
								result.repeat(" ", spaces).append(cell.builder);
							}
						}
					}
					else if (x < this.maxX) {
						result.repeat(" ", spaces);
					}
				}
				result.append('\n');
			}
			result.setLength(Math.max(result.length() - 1, 0));
		}
		return result;
	}

	@Override
	public String toString() {
		return this.toStringBuilder().toString();
	}

	public void clear() {
		this.table.clear();
		this.maxX = this.maxY = -1;
	}

	public static enum Justification {
		LEFT,
		RIGHT;
	}

	public static class Cell {

		public final StringBuilder builder;
		public Justification justification = Justification.LEFT;

		public Cell() {
			this.builder = new StringBuilder();
		}

		public Cell append(byte         value) { this.builder.append(value); return this; }
		public Cell append(short        value) { this.builder.append(value); return this; }
		public Cell append(int          value) { this.builder.append(value); return this; }
		public Cell append(long         value) { this.builder.append(value); return this; }
		public Cell append(float        value) { this.builder.append(value); return this; }
		public Cell append(double       value) { this.builder.append(value); return this; }
		public Cell append(char         value) { this.builder.append(value); return this; }
		public Cell append(boolean      value) { this.builder.append(value); return this; }
		public Cell append(CharSequence value) { this.builder.append(value); return this; }
		public Cell append(Object       value) { this.builder.append(value); return this; }
		public Cell justify(Justification justification) { this.justification = justification; return this; }
		public int length() { return this.builder.length(); }

		@Override
		public String toString() {
			return this.builder.toString();
		}
	}
}