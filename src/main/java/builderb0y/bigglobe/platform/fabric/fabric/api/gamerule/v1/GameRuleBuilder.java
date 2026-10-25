package builderb0y.bigglobe.platform.fabric.fabric.api.gamerule.v1;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public class GameRuleBuilder<T> {

	private final T defaultValue;
	private final boolean isBoolean;
	private final boolean isDouble;

	private GameRuleBuilder(T defaultValue, boolean isBoolean, boolean isDouble) {
		this.defaultValue = defaultValue;
		this.isBoolean = isBoolean;
		this.isDouble = isDouble;
	}

	public static GameRuleBuilder<Boolean> forBoolean(boolean defaultValue) {
		return new GameRuleBuilder<>(defaultValue, true, false);
	}

	public static GameRuleBuilder<Double> forDouble(double defaultValue) {
		return new GameRuleBuilder<>(defaultValue, false, true);
	}

	@SuppressWarnings("unchecked")
	private static <T extends GameRules.Value<T>> GameRules.Type<T> createCustomType(
		Supplier<ArgumentType<?>> argument,
		Function<GameRules.Type<T>, T> constructor,
		BiConsumer<MinecraftServer, T> callback
	) {
		try {
			Class<?> visitorCallerClass = Class.forName("net.minecraft.world.level.GameRules$VisitorCaller");
			Object visitorCaller = Proxy.newProxyInstance(
				GameRules.class.getClassLoader(),
				new Class<?>[] { visitorCallerClass },
				(proxy, method, args) -> null
			);
			Constructor<?> ctor = GameRules.Type.class.getDeclaredConstructor(
				Supplier.class, Function.class, BiConsumer.class, visitorCallerClass
			);
			ctor.setAccessible(true);
			return (GameRules.Type<T>) ctor.newInstance(argument, constructor, callback, visitorCaller);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to instantiate GameRules.Type", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <K extends GameRules.Value<K>> GameRules.Key<K> buildAndRegister(ResourceLocation id) {
		if (this.isBoolean) {
			GameRules.Type<GameRules.BooleanValue> type = GameRules.BooleanValue.create((Boolean)this.defaultValue, (server, value) -> {
				GameRuleEvents.fire(id, value.get(), server);
			});
			return (GameRules.Key<K>)GameRules.register(id.toString().replace(':', '.'), GameRules.Category.MISC, type);
		}
		else {
			GameRules.Type<DoubleValue> type = createCustomType(
				() -> DoubleArgumentType.doubleArg(),
				(t) -> new DoubleValue(t, (Double)this.defaultValue, id),
				(server, value) -> GameRuleEvents.fire(id, value.get(), server)
			);
			return (GameRules.Key<K>)GameRules.register(id.toString().replace(':', '.'), GameRules.Category.MISC, type);
		}
	}

	public static class DoubleValue extends GameRules.Value<DoubleValue> {
		private double value;
		private final ResourceLocation id;

		public DoubleValue(GameRules.Type<DoubleValue> type, double defaultValue, ResourceLocation id) {
			super(type);
			this.value = defaultValue;
			this.id = id;
		}

		@Override
		protected void updateFromArgument(CommandContext<CommandSourceStack> context, String paramName) {
			this.value = DoubleArgumentType.getDouble(context, paramName);
		}

		public double get() {
			return this.value;
		}

		public void set(double value, MinecraftServer server) {
			this.value = value;
			this.onChanged(server);
		}

		@Override
		public String serialize() {
			return Double.toString(this.value);
		}

		@Override
		protected void deserialize(String value) {
			try {
				this.value = Double.parseDouble(value);
			}
			catch (NumberFormatException ignored) {}
		}

		@Override
		public int getCommandResult() {
			return (int)this.value;
		}

		@Override
		protected DoubleValue getSelf() {
			return this;
		}

		@Override
		protected DoubleValue copy() {
			DoubleValue copy = new DoubleValue(this.type, this.value, this.id);
			copy.value = this.value;
			return copy;
		}

		@Override
		public void setFrom(DoubleValue value, MinecraftServer server) {
			this.value = value.value;
			this.onChanged(server);
		}
	}
}
