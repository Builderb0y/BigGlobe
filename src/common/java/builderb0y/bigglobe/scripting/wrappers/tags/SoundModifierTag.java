package builderb0y.bigglobe.scripting.wrappers.tags;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.entries.SoundModifierEntry;
import builderb0y.bigglobe.sounds.SoundModifier;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.AbstractConstantFactory;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class SoundModifierTag extends TagWrapper<SoundModifier, SoundModifierEntry> {

	public static final TypeInfo TYPE = type(SoundModifierTag.class);
	public static final TagParser PARSER = new TagParser("SoundModifierTag", SoundModifierTag.class, "SoundModifier", MethodInfo.findMethod(SoundModifierEntry.class, "isIn", boolean.class, SoundModifierTag.class));

	public SoundModifierTag(DelayedEntryList<SoundModifier> list) {
		super(list);
	}

	public static SoundModifierTag of(MethodHandles.Lookup caller, String name, Class<?> type, int flags, String... ids) {
		return of(flags, ids);
	}

	public static SoundModifierTag of(int flags, String... ids) {
		return new SoundModifierTag(DelayedEntryList.create(BigGlobeDynamicRegistries.SOUND_MODIFIER_REGISTRY_KEY, (flags & AbstractConstantFactory.CLIENT) != 0, ids));
	}

	@Override
	public SoundModifierEntry wrap(Holder<SoundModifier> entry) {
		return new SoundModifierEntry(entry);
	}

	@Override
	public Holder<SoundModifier> unwrap(SoundModifierEntry entry) {
		return entry.entry;
	}

	@Override
	public boolean contains(SoundModifierEntry entry) {
		return super.contains(entry);
	}

	@Override
	public SoundModifierEntry random(long seed) {
		return super.random(seed);
	}

	@Override
	public SoundModifierEntry random(RandomGenerator random) {
		return super.random(random);
	}
}