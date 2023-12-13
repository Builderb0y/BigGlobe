package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.registry.RegistryKey;
import net.minecraft.state.property.Property;
import net.minecraft.world.poi.PointOfInterestTypes;

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
import builderb0y.bigglobe.versions.BlockArgumentParserVersions;

public class BlockStateCoder extends NamedCoder<BlockState> {

	public static final BlockStateCoder INSTANCE = new BlockStateCoder("BlockStateCoder.INSTANCE");

	public BlockStateCoder(String toString) {
		super(toString);
	}

	public static <T_Encoded> void verifyNormal(VerifyContext<T_Encoded, BlockState> context) throws VerifyException {
		BlockState state = context.object;
		#if MC_VERSION >= MC_1_20_0
			if (state != null && (state.hasBlockEntity() || PointOfInterestTypes.getTypeForState(state).isPresent())) {
		#else
			if (state != null && (state.getLuminance() != 0 || state.hasBlockEntity() || PointOfInterestTypes.getTypeForState(state).isPresent())) {
		#endif
			throw new VerifyException(() -> {
				StringBuilder message = new StringBuilder("For technical reasons, ");
				context.appendPathTo(message);
				#if MC_VERSION >= MC_1_20_0
					return message.append(" cannot have a BlockEntity or be a point of interest. (was ").append(state).append(')').toString();
				#else
					return message.append(" cannot emit light, have a BlockEntity, or be a point of interest. (was ").append(state).append(')').toString();
				#endif
			});
		}
	}

	@Target(ElementType.TYPE_USE)
	@Retention(RetentionPolicy.RUNTIME)
	@UseVerifier(name = "verifyNormal", in = BlockStateCoder.class, usage = MemberUsage.METHOD_IS_HANDLER)
	@Mirror(UseVerifier.class)
	public static @interface VerifyNormal {}

	@Override
	public <T_Encoded> @Nullable BlockState decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		String string = context.tryAsString();
		if (string != null) try {
			BlockResult result = BlockArgumentParserVersions.block(string, false);
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
			return context.logger().unwrapLazy(
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
		else throw new EncodeException(() -> "Unregistered block: " + state);
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