package builderb0y.bigglobe.mixinInterfaces;

import java.util.Comparator;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.ColumnValue.CustomDisplayContext;
import builderb0y.bigglobe.columns.WorldColumn;

public interface ColumnValueDisplayer {

	public abstract ColumnValue<?>[] bigglobe_getDisplayedColumnValues();

	public abstract void bigglobe_setDisplayedColumnValues(ColumnValue<?>[] displayedColumnValues);

	public default void bigglobe_appendText(List<String> text, WorldColumn column, int y) {
		PlayerEntity player = bigglobe_getClientPlayer();
		CustomDisplayContext context = player == null ? null : new CustomDisplayContext(player, column, y);
		if (this.bigglobe_getDisplayedColumnValues() == null) {
			this.bigglobe_setDisplayedColumnValues(
				ColumnValue.ARRAY_FACTORY.empty()
			);
		}
		for (ColumnValue<?> value : this.bigglobe_getDisplayedColumnValues()) {
			text.add(
				value == null
				? ""
				: context != null
				? value.getName() + ": " + value.getDisplayText(context)
				: value.getName() + ": " + CustomDisplayContext.format(value.getValue(column, y))
			);
		}
	}

	public static @Nullable PlayerEntity bigglobe_getClientPlayer() {
		return switch (FabricLoader.getInstance().getEnvironmentType()) {
			case CLIENT -> bigglobe_getClientPlayer0();
			case SERVER -> null;
		};
	}

	@Environment(EnvType.CLIENT)
	public static PlayerEntity bigglobe_getClientPlayer0() {
		return MinecraftClient.getInstance().player;
	}
}