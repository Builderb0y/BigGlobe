package builderb0y.bigglobe.chunkgen.scripted;

import builderb0y.bigglobe.chunkgen.scripted.AbstractObjectSegmentList.ObjectSegment;

public class ObjectSegmentList<T> extends AbstractObjectSegmentList<T, ObjectSegment<T>> {

	public ObjectSegmentList(int minY, int maxY) {
		super(minY, maxY);
	}

	@Override
	public ObjectSegment<T> newSegment(int minY, int maxY) {
		return new ObjectSegment<>(minY, maxY);
	}
}