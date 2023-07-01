package builderb0y.bigglobe.codecs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryEntryList;
import net.minecraft.tag.TagKey;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.common.FactoryException;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.imprinters.AutoImprinter;
import builderb0y.autocodec.imprinters.AutoImprinter.NamedImprinter;
import builderb0y.autocodec.imprinters.ImprintContext;
import builderb0y.autocodec.imprinters.ImprintException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.versions.BlockArgumentParserVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.bigglobe.versions.RegistryVersions;

public class BlockStateCollectionImprinter extends NamedImprinter<Collection<BlockState>> {

	public static final BlockStateCollectionImprinter INSTANCE = new BlockStateCollectionImprinter();

	public BlockStateCollectionImprinter() {
		super("BlockStateCollectionImprinter");
	}

	@Override
	public <T_Encoded> void imprint(@NotNull ImprintContext<T_Encoded, Collection<BlockState>> context) throws ImprintException {
		if (context.isEmpty()) return;
		for (DecodeContext<T_Encoded> element : context.tryAsList(true)) {
			if (element.isEmpty()) continue;
			this.imprintEntry(new ImprintContext<>(element, context.object));
		}
	}

	public <T_Encoded> void imprintEntry(ImprintContext<T_Encoded, Collection<BlockState>> context) throws ImprintException {
		String string = context.tryAsString();
		if (string != null) {
			this.imprintAsString(context, string);
		}
		else {
			this.imprintAsObject(context);
		}
	}

	public <T_Encoded> void imprintAsString(ImprintContext<T_Encoded, Collection<BlockState>> context, String string) throws ImprintException {
		try {
			BlockArgumentParserVersions
			.blockOrTag(string, false)
			.ifLeft(blockResult -> {
				if (blockResult.blockState().getProperties().size() == blockResult.properties().size()) {
					context.object.add(blockResult.blockState());
				}
				else if (blockResult.properties().isEmpty()) {
					context.object.addAll(blockResult.blockState().getBlock().getStateManager().getStates());
				}
				else {
					this.filterAndAdd(context.object, blockResult.blockState().getBlock(), blockResult.properties());
				}
			})
			.ifRight(tagResult -> {
				this.filterAndAdd(context.object, tagResult.tag(), tagResult.vagueProperties());
			});
		}
		catch (CommandSyntaxException exception) {
			throw new ImprintException(exception);
		}
	}

	public <T_Encoded> void imprintAsObject(ImprintContext<T_Encoded, Collection<BlockState>> context) throws ImprintException {
		try {
			DecodeContext<T_Encoded> name = context.getMember(State.NAME);
			if (!name.isEmpty()) {
				Identifier id = new Identifier(name.forceAsString());
				this.imprintAsObjectName(context, id);
			}
			else {
				DecodeContext<T_Encoded> tag = context.getMember("Tag");
				if (!tag.isEmpty()) {
					Identifier id = new Identifier(tag.forceAsString());
					this.imprintAsObjectTag(context, id);
				}
				else {
					throw new ImprintException("Must specify " + State.NAME + " or Tag.");
				}
			}
		}
		catch (ImprintException imprintException) {
			throw imprintException;
		}
		catch (DecodeException | InvalidIdentifierException exception) {
			throw new ImprintException(exception);
		}
	}

	public <T_Encoded> void imprintAsObjectName(ImprintContext<T_Encoded, Collection<BlockState>> context, Identifier id) throws DecodeException {
		if (!RegistryVersions.block().containsId(id)) {
			throw new DecodeException("Unknown block: " + id);
		}
		Block block = RegistryVersions.block().get(id);
		Map<String, String> stringProperties = this.getObjectProperties(context);
		if (stringProperties.isEmpty()) {
			context.object.addAll(block.getStateManager().getStates());
		}
		else {
			Map<Property<?>, Comparable<?>> properties = this.convertProperties(block, stringProperties);
			if (properties != null) {
				this.filterAndAdd(context.object, block, properties);
			}
		}
	}

	public <T_Encoded> void imprintAsObjectTag(ImprintContext<T_Encoded, Collection<BlockState>> context, Identifier tagID) throws DecodeException {
		TagKey<Block> tagKey = TagKey.of(RegistryKeyVersions.block(), tagID);
		RegistryEntryList<Block> tagEntries = RegistryVersions.block().getEntryList(tagKey).orElse(null);
		if (tagEntries == null) throw new ImprintException("No such tag " + tagID + " in registry " + RegistryKeyVersions.block().getValue());
		Map<String, String> stringProperties = this.getObjectProperties(context);
		this.filterAndAdd(context.object, tagEntries, stringProperties);
	}

	public void filterAndAdd(Collection<BlockState> states, Iterable<? extends RegistryEntry<Block>> entries, Map<String, String> stringProperties) {
		if (stringProperties.isEmpty()) {
			for (RegistryEntry<Block> block : entries) {
				states.addAll(block.value().getStateManager().getStates());
			}
		}
		else {
			for (RegistryEntry<Block> block : entries) {
				Map<Property<?>, Comparable<?>> properties = this.convertProperties(block.value(), stringProperties);
				if (properties == null) continue;
				this.filterAndAdd(states, block.value(), properties);
			}
		}
	}

	public void filterAndAdd(Collection<BlockState> states, Block block, Map<Property<?>, Comparable<?>> properties) {
		nextState:
		for (BlockState state : block.getStateManager().getStates()) {
			for (Map.Entry<Property<?>, Comparable<?>> entry : properties.entrySet()) {
				if (state.get(entry.getKey()) != entry.getValue()) {
					continue nextState;
				}
			}
			states.add(state);
		}
	}

	public <T_Encoded> Map<String, String> getObjectProperties(DecodeContext<T_Encoded> context) throws DecodeException {
		DecodeContext<T_Encoded> propertiesContext = context.getMember(State.PROPERTIES);
		return (
			propertiesContext.isEmpty()
			? Collections.emptyMap()
			: (
				propertiesContext
				.forceAsStringMap()
				.entrySet()
				.stream()
				.collect(
					Collectors.toMap(
						Map.Entry::getKey,
						entry -> {
							try { return entry.getValue().forceAsString(); }
							catch (DecodeException exception) { throw AutoCodecUtil.rethrow(exception); }
						}
					)
				)
			)
		);
	}

	public Map<Property<?>, Comparable<?>> convertProperties(Block block, Map<String, String> from) {
		StateManager<Block, BlockState> manager = block.getStateManager();
		Map<Property<?>, Comparable<?>> properties = new HashMap<>(from.size());
		for (Map.Entry<String, String> vagueEntry : from.entrySet()) {
			Property<?> property = manager.getProperty(vagueEntry.getKey());
			if (property == null) return null;
			Comparable<?> value = property.parse(vagueEntry.getValue()).orElse(null);
			if (value == null) return null;
			properties.put(property, value);
		}
		return properties;
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