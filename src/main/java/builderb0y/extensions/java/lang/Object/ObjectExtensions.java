package builderb0y.extensions.java.lang.Object;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class ObjectExtensions {

	@SuppressWarnings("unchecked")
	public static <T> T as(@This Object object) {
		return (T)(object);
	}
}