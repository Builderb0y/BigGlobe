package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DynamicOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Property;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;

public class BlockStateCoder extends NamedCoder<BlockState> {

	public static final BlockStateCoder INSTANCE = new BlockStateCoder("BlockStateCoder.INSTANCE");

	public BlockStateCoder(String toString) {
		super(toString);
	}

	public static <T_Encoded> void verifyNormal(VerifyContext<T_Encoded, BlockState> context) throws VerifyException {
		BlockState state = context.object;
		if (state != null && (state.getLuminance() != 0 || state.hasBlockEntity())) {
			StringBuilder message = new StringBuilder("For technical reasons, ");
			context.appendPathTo(message);
			throw new VerifyException(message.append(" cannot emit light or have a BlockEntity. (was ").append(state).append(')').toString());
		}
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	@UseVerifier(name = "verifyNormal", in = BlockStateCoder.class, usage = MemberUsage.METHOD_IS_HANDLER)
	@Mirror(UseVerifier.class)
	public static @interface VerifyNormal {}

	@SuppressWarnings("unchecked")
	public static <X extends Throwable> RegistryWrapper<Block> getBlockRegistry(DynamicOps<?> ops, Function<String, X> exceptionFactory) throws X {
		if (ops instanceof RegistryOps<?> registryOps) {
			if (registryOps.getEntryLookup(RegistryKeys.BLOCK).orElse(null) instanceof RegistryWrapper<Block> wrapper) {
				return wrapper;
			}
			if (registryOps.getOwner(RegistryKeys.BLOCK).orElse(null) instanceof RegistryWrapper<?> wrapper) {
				return (RegistryWrapper<Block>)(wrapper);
			}
			throw exceptionFactory.apply("Unable to access registry " + RegistryKeys.BLOCK.getValue() + " in " + registryOps);
		}
		else {
			throw exceptionFactory.apply("Not a RegistryOps: " + ops);
		}
	}

	@Override
	public <T_Encoded> @Nullable BlockState decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String string = context.tryAsString();
		if (string != null) try {
			BlockResult result = BlockArgumentParser.block(getBlockRegistry(context.ops, DecodeException::new), string, false);
			Set<Property<?>> missing = new HashSet<>(result.blockState().getProperties());
			missing.removeAll(result.properties().keySet());
			if (!missing.isEmpty()) {
				context.logger().logErrorLazy(() -> "Missing properties: " + missing);
			}
			return result.blockState();
		}
		catch (CommandSyntaxException exception) {
			throw new DecodeException(exception);
		}
		else {
			return context.logger().unwrap(
				BlockState.CODEC.parse(context.ops, context.input),
				true,
				DecodeException::new
			);
		}
	}

	@Override
	public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, BlockState> context) throws EncodeException {
		BlockState state = context.input;
		if (state == null) return context.empty();
		StringBuilder builder = new StringBuilder(64);
		Optional<RegistryKey<Block>> key = state.getRegistryEntry().getKey();
		if (key.isPresent()) builder.append(key.get().getValue());
		else throw new EncodeException("Unregistered block.");
		Collection<Property<?>> properties = state.getProperties();
		if (!properties.isEmpty()) {
			builder.append('[');
			Iterator<Property<?>> iterator = properties.iterator();
			appendProperty(builder, state, iterator.next());
			while (iterator.hasNext()) {
				appendProperty(builder.append(','), state, iterator.next());
			}
			builder.append(']');
		}
		return context.createString(builder.toString());
	}

	public static <T extends Comparable<T>> void appendProperty(StringBuilder builder, BlockState state, Property<T> property) {
		builder.append(property.getName()).append('=').append(property.name(state.get(property)));
	}
}