package builderb0y.bigglobe.codecs;

import java.util.Collection;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.imprinters.AutoImprinter;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.registries.AbstractRegistryCoder;

public class BlockStateCollectionImprinter extends NamedImprinter<Collection<BlockState>> {

	public static final BlockStateCollectionImprinter INSTANCE = new BlockStateCollectionImprinter();

	public BlockStateCollectionImprinter() {
		super("BlockStateCollectionImprinter");
	}

	@Override
	public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, Collection<BlockState>> context) throws ImprintException {
		if (context.isEmpty()) return;
		for (ImprintContext<T_Encoded, Collection<BlockState>> element : context.listIterableOrSingleton()) {
			if (element.isEmpty()) continue;
			this.imprintEntry(new ImprintContext<>(element, context.object));
		}
	}

	public <T_Encoded> void imprintEntry(ImprintContext<T_Encoded, Collection<BlockState>> context) throws ImprintException {
		try {
			BlockStateCoder
				.decodeBlockOrTag(AbstractRegistryCoder.registry(Registries.BLOCK, context), context.forceAsString().value)
				.unwrapLazy(context.logger()::logErrorLazy, ImprintException::new)
				.getMatchingStates()
				.forEach(context.object::add);
		}
		catch (DecodeException exception) {
			throw new ImprintException(exception);
		}
	}

	public static class Factory extends NamedImprinterFactory {

		public static final Factory INSTANCE = new Factory();

		@Override
		public <T_HandledType> @Nullable AutoImprinter<?> tryCreate(@NotNull FactoryContext<T_HandledType> context) throws FactoryException {
			ReifiedType<?> elementType = context.type.getUpperBoundOrSelf().resolveParameter(Collection.class);
			if (elementType != null && elementType.getRawClass() == BlockState.class) {
				return BlockStateCollectionImprinter.INSTANCE;
			}
			return null;
		}
	}
}