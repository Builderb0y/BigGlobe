package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

import builderb0y.bigglobe.scripting.wrappers.tags.EntityTypeTag;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class EntityTypeEntry extends EntryWrapper<EntityType<?>, EntityTypeTag> {

	public static final TypeInfo TYPE = TypeInfo.of(EntityTypeEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public EntityTypeEntry(Holder<EntityType<?>> entry) {
		super(entry);
	}

	public static EntityTypeEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static EntityTypeEntry of(String id, int flags) {
		Holder<EntityType<?>> holder = ConstantFactory.getEntry(Registries.ENTITY_TYPE, id, flags);
		return holder != null ? new EntityTypeEntry(holder) : null;
	}

	@Override
	public boolean isIn(EntityTypeTag tag) {
		return super.isIn(tag);
	}
}