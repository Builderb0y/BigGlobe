package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;

import net.minecraft.util.StringIdentifiable;

import builderb0y.autocodec.common.Case;
import builderb0y.autocodec.common.EnumName;

public class StringIdentifiableEnumName implements EnumName {

	public static final StringIdentifiableEnumName INSTANCE = new StringIdentifiableEnumName();

	@Override
	public @NotNull String getEnumName(@NotNull Enum<?> value) {
		if (value instanceof StringIdentifiable identifiable) {
			return identifiable.asString();
		}
		else {
			return Case.LOWER_SNAKE_CASE.apply(value.name());
		}
	}
}