package builderb0y.bigglobe.mixinInterfaces;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;

/** implemented on {@link BlockView} via mixin. */
public interface DimensionalBlockView {

	public static final ClassValue<@Nullable Field> DELEGATE = new ClassValue<>() {

		@Override
		public @Nullable Field computeValue(Class<?> type) {
			Field found = null;
			while (type != null) {
				for (Field field : type.getDeclaredFields()) {
					if (!Modifier.isStatic(field.getModifiers()) && World.class.isAssignableFrom(field.getType())) {
						if (found == null) {
							found = field;
						}
						else {
							BigGlobeMod.LOGGER.warn("Found multiple World delegates on " + type);
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
				type = type.getSuperclass();
			}
			BigGlobeMod.LOGGER.warn("Unable to find field of type World on " + type);
			return null;
		}
	};

	public default @Nullable RegistryKey<World> bigglobe_getDimension() {
		if (this instanceof World world) {
			return world.getRegistryKey();
		}
		else {
			Field field = DELEGATE.get(this.getClass());
			if (field != null) try {
				return ((World)(field.get(this))).getRegistryKey();
			}
			catch (ReflectiveOperationException exception) {
				BigGlobeMod.LOGGER.error("Could not access " + field + " but it was previously marked as accessible?", exception);
			}
			return null;
		}
	}
}