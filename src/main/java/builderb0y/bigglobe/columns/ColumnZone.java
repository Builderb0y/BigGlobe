package builderb0y.bigglobe.columns;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.settings.Seed;

public class ColumnZone<T> {

	public final T value;
	public final RestrictedColumnZone<T> @SingletonArray @VerifyNullable [] children;

	@SafeVarargs
	public ColumnZone(T value, RestrictedColumnZone<T>... children) {
		this.value = value;
		this.children = children;
	}

	public T value() {
		return this.value;
	}

	public T get(WorldColumn column, double y) {
		if (this.children != null) {
			for (RestrictedColumnZone<T> child : this.children) {
				if (child.restriction.test(column, y, child.seed.xor(column.seed))) {
					return child.get(column, y);
				}
			}
		}
		return this.value;
	}

	/*
	public boolean dependsOnY(WorldColumn column) {
		if (this.children != null) {
			for (RestrictedColumnZone<T> child : this.children) {
				if (child.dependsOnY(column)) return true;
			}
		}
		return false;
	}
	*/

	public void forEachZone(Consumer<? super ColumnZone<T>> action) {
		action.accept(this);
		if (this.children != null) {
			for (RestrictedColumnZone<T> child : this.children) {
				child.forEachZone(action);
			}
		}
	}

	public Stream<ColumnZone<T>> streamZones() {
		if (this.children != null) {
			return Stream.concat(
				Stream.of(this),
				Arrays.stream(this.children).flatMap(RestrictedColumnZone::streamZones)
			);
		}
		else {
			return Stream.of(this);
		}
	}

	public static class RestrictedColumnZone<T> extends ColumnZone<T> {

		public final ColumnRestriction restriction;
		public final Seed seed;

		@SafeVarargs
		public RestrictedColumnZone(T value, ColumnRestriction restriction, Seed seed, RestrictedColumnZone<T>... children) {
			super(value, children);
			this.restriction = restriction;
			this.seed = seed;
		}

		/*
		@Override
		public boolean dependsOnY(WorldColumn column) {
			return this.restriction.dependsOnY(column) || super.dependsOnY(column);
		}
		*/
	}
}