package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.entries.SpawnTweakerEntry;
import builderb0y.bigglobe.spawning.SpawnTweaker;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SpawnTweakerTag extends TagWrapper<SpawnTweaker, SpawnTweakerEntry> {

	public static final TypeInfo TYPE = type(SpawnTweakerTag.class);
	public static final TagParser PARSER = new TagParser("SpawnTweakerTag", SpawnTweakerTag.class, "SpawnTweaker", MethodInfo.findMethod(SpawnTweakerEntry.class, "isIn", boolean.class, SpawnTweakerTag.class));

	public SpawnTweakerTag(DelayedEntryList<SpawnTweaker> list) {
		super(list);
	}

	public static SpawnTweakerTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static SpawnTweakerTag of(int flags, String... ids) {
		return new SpawnTweakerTag(DelayedEntryList.create(BigGlobeDynamicRegistries.MOB_SPAWN_TWEAKER_REGISTRY_KEY, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public SpawnTweakerEntry wrap(Holder<SpawnTweaker> entry) {
		return new SpawnTweakerEntry(entry);
	}

	@Override
	public Holder<SpawnTweaker> unwrap(SpawnTweakerEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(SpawnTweakerEntry entry) {
		return super.contains(entry);
	}

	@Override
	public SpawnTweakerEntry random(long seed) {
		return super.random(seed);
	}

	@Override
	public SpawnTweakerEntry random(RandomGenerator random) {
		return super.random(random);
	}
}