package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.tags.SpawnTweakerTag;
import builderb0y.bigglobe.spawning.SpawnTweaker;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class SpawnTweakerEntry extends EntryWrapper<SpawnTweaker, SpawnTweakerTag> {

	public static final TypeInfo TYPE = TypeInfo.of(SpawnTweakerEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public SpawnTweakerEntry(Holder<SpawnTweaker> entry) {
		super(entry);
	}

	public static SpawnTweakerEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static SpawnTweakerEntry of(String id, int flags) {
		Holder<SpawnTweaker> holder = ConstantFactory.getEntry(BigGlobeDynamicRegistries.MOB_SPAWN_TWEAKER_REGISTRY_KEY, id, flags);
		return holder != null ? new SpawnTweakerEntry(holder) : null;
	}

	@Override
	public boolean isIn(SpawnTweakerTag tag) {
		return super.isIn(tag);
	}
}