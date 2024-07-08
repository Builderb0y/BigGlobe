package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record ItemTagKey(TagKey<Item> key) implements TagWrapper<Item, Item> {

	public static final TypeInfo TYPE = type(ItemTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static ItemTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static ItemTagKey of(String id) {
		if (id == null) return null;
		return new ItemTagKey(TagKey.of(RegistryKeyVersions.item(), IdentifierVersions.create(id)));
	}

	@Override
	public Item wrap(RegistryEntry<Item> entry) {
		return entry.value();
	}

	@Override
	public Item random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public Item random(long seed) {
		return this.randomImpl(seed);
	}
}