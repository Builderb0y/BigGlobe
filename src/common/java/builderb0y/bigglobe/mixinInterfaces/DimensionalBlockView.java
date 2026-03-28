package builderb0y.bigglobe.mixinInterfaces;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import builderb0y.bigglobe.BigGlobeMod;

/**
implemented on {@link BlockGetter} via mixin.
*/
public interface DimensionalBlockView {

	public static final ClassValue<@Nullable Field> DELEGATE = new ClassValue<>() {

		@Override
		public @Nullable Field computeValue(Class<?> type) {
			Field found = null;
			for (Class<?> search = type; search != null; search = search.getSuperclass()) {
				for (Field field : search.getDeclaredFields()) {
					if (!Modifier.isStatic(field.getModifiers()) && Level.class.isAssignableFrom(field.getType())) {
						if (found == null) {
							found = field;
						}
						else {
							BigGlobeMod.LOGGER.warn("Found multiple World delegates on " + search);
							return null;
						}
					}
				}
				if (found != null) {
					if (found.trySetAccessible()) {
						BigGlobeMod.LOGGER.info("Automatically found World delegate: " + found);
						return found;
					}
					else {
						BigGlobeMod.LOGGER.warn("Could not access " + found);
					}
				}
			}
			BigGlobeMod.LOGGER.warn("Unable to find field of type World on " + type);
			return null;
		}
	};

	public default @Nullable ResourceKey<Level> bigglobe_getDimension() {
		if (this instanceof Level world) {
			return world.dimension();
		}
		else {
			Field field = DELEGATE.get(this.getClass());
			if (field != null) try {
				return ((Level)(field.get(this))).dimension();
			}
			catch (ReflectiveOperationException exception) {
				BigGlobeMod.LOGGER.error("Could not access " + field + " but it was previously marked as accessible?", exception);
			}
			return null;
		}
	}
}