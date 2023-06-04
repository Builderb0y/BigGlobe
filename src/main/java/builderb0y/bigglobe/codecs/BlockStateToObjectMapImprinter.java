package builderb0y.bigglobe.codecs;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;

import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.AutoDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.imprinters.AutoImprinter;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public class BlockStateToObjectMapImprinter<V, M extends Map<BlockState, V>> extends NamedImprinter<M> {

	public final AutoDecoder<V> valueDecoder;

	public BlockStateToObjectMapImprinter(@NotNull ReifiedType<M> type, AutoDecoder<V> valueDecoder) {
		super(type);
		this.valueDecoder = valueDecoder;
	}

	@Override
	public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, M> context) throws ImprintException {
		try {
			for (Map.Entry<String, DecodeContext<T_Encoded>> entry : context.forceAsStringMap().entrySet()) {
				BlockResult keys = BlockArgumentParser.block(Registry.BLOCK, entry.getKey(), false);
				V value = entry.getValue().decodeWith(this.valueDecoder);
				keys
				.blockState()
				.getBlock()
				.getStateManager()
				.getStates()
				.stream()
				.filter((BlockState state) -> (
					keys
					.properties()
					.entrySet()
					.stream()
					.allMatch((Map.Entry<Property<?>, Comparable<?>> propertyMapping) -> (
						state
						.get(propertyMapping.getKey())
						.equals(propertyMapping.getValue())
					))
				))
				.forEach(state -> context.object.put(state, value));
			}
		}
		catch (ImprintException exception) {
			throw exception;
		}
		catch (Exception exception) {
			throw new ImprintException(exception);
		}
	}

	public static class Factory extends NamedImprinterFactory {

		public static final Factory INSTANCE = new Factory();

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public @Nullable <T_HandledType> AutoImprinter<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
			ReifiedType<?>[] keyValueTypes = context.type.getUpperBoundOrSelf().resolveParameters(Map.class);
			if (keyValueTypes != null && keyValueTypes[0].getRawClass() == BlockState.class) {
				return new BlockStateToObjectMapImprinter(context.type, context.type(keyValueTypes[1]).forceCreateDecoder());
			}
			return null;
		}
	}
}