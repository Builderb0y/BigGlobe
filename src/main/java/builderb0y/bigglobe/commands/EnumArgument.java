package builderb0y.bigglobe.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import builderb0y.bigglobe.commands.EnumArgument.EnumArgumentSerializer.EnumArgumentProperties;

/** {@link EnumArgumentType} is needlessly complex for what it does. */
public class EnumArgument<E extends Enum<E> & StringIdentifiable> implements ArgumentType<E> {

	public static final DynamicCommandExceptionType INVALID_ENUM = new DynamicCommandExceptionType(
		object -> Text.translatable("argument.enum.invalid", object)
	);

	public final Class<E> enumClass;
	public final E[] enumValues;
	public final Map<String, E> lookup;

	public EnumArgument(Class<E> enumClass) {
		this.enumClass = enumClass;
		this.enumValues = enumClass.getEnumConstants();
		this.lookup = new HashMap<>(this.enumValues.length);
		for (E value : this.enumValues) {
			this.lookup.put(value.asString(), value);
		}
	}

	@Override
	public E parse(StringReader reader) throws CommandSyntaxException {
		String name = reader.readUnquotedString();
		E value = this.lookup.get(name);
		if (value != null) return value;
		else throw INVALID_ENUM.createWithContext(reader, name);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(this.lookup.keySet(), builder);
	}

	public static class EnumArgumentSerializer implements ArgumentSerializer<EnumArgument<?>, EnumArgumentProperties> {

		@Override
		public void writePacket(EnumArgumentProperties properties, PacketByteBuf buf) {
			buf.writeString(properties.enumClass.getName());
		}

		@Override
		public EnumArgumentProperties fromPacket(PacketByteBuf buf) {
			try {
				return this.new EnumArgumentProperties(Class.forName(buf.readString(), false, EnumArgumentSerializer.class.getClassLoader()).asSubclass(Enum.class));
			}
			catch (ClassNotFoundException exception) {
				throw new IllegalStateException(exception);
			}
		}

		@Override
		public void writeJson(EnumArgumentProperties properties, JsonObject json) {
			json.addProperty("enumClass", properties.enumClass.getName());
		}

		@Override
		public EnumArgumentProperties getArgumentTypeProperties(EnumArgument<?> argumentType) {
			return this.new EnumArgumentProperties(argumentType.enumClass);
		}

		public class EnumArgumentProperties implements ArgumentTypeProperties<EnumArgument<?>> {

			public final Class<?> enumClass;

			public EnumArgumentProperties(Class<?> enumClass) {
				this.enumClass = enumClass.asSubclass(Enum.class).asSubclass(StringIdentifiable.class);
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public EnumArgument<?> createType(CommandRegistryAccess commandRegistryAccess) {
				return new EnumArgument(this.enumClass);
			}

			@Override
			public ArgumentSerializer<EnumArgument<?>, ?> getSerializer() {
				return EnumArgumentSerializer.this;
			}
		}
	}
}