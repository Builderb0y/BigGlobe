package builderb0y.bigglobe.codecs;

import org.jetbrains.annotations.NotNull;
import builderb0y.autocodec.common.Case;
import builderb0y.autocodec.common.EnumName;
import net.minecraft.util.StringRepresentable;

public class StringIdentifiableEnumName implements EnumName {

	public static final StringIdentifiableEnumName INSTANCE = new StringIdentifiableEnumName();

	@Override
	public @NotNull String getEnumName(@NotNull Enum<?> value) {
		if (value instanceof StringRepresentable identifiable) {
			return identifiable.getSerializedName();
		}
		else {
			return Case.LOWER_SNAKE_CASE.apply(value.name());
		}
	}
}