package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

import builderb0y.bigglobe.scripting.wrappers.entries.EntityTypeEntry;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class EntityTypeTag extends TagWrapper<EntityType<?>, EntityTypeEntry> {

	public static final TypeInfo TYPE = type(EntityTypeTag.class);
	public static final TagParser PARSER = new TagParser("EntityTypeTag", EntityTypeTag.class, "EntityType", MethodInfo.findMethod(EntityTypeEntry.class, "isIn", boolean.class, EntityTypeTag.class));

	public EntityTypeTag(DelayedEntryList<EntityType<?>> list) {
		super(list);
	}

	public static EntityTypeTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static EntityTypeTag of(int flags, String... ids) {
		return new EntityTypeTag(DelayedEntryList.create(Registries.ENTITY_TYPE, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public EntityTypeEntry wrap(Holder<EntityType<?>> entry) {
		return new EntityTypeEntry(entry);
	}

	@Override
	public Holder<EntityType<?>> unwrap(EntityTypeEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(EntityTypeEntry entry) {
		return super.contains(entry);
	}

	@Override
	public EntityTypeEntry random(long seed) {
		return super.random(seed);
	}

	@Override
	public EntityTypeEntry random(RandomGenerator random) {
		return super.random(random);
	}
}