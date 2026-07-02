package builderb0y.bigglobe.scripting.wrappers.entries;

import java.lang.invoke.MethodHandles;

import net.minecraft.core.Holder;

import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.scripting.wrappers.tags.SoundModifierTag;
import builderb0y.bigglobe.sounds.SoundModifier;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.scripting.bytecode.TypeInfo;

public class SoundModifierEntry extends EntryWrapper<SoundModifier, SoundModifierTag> {

	public static final TypeInfo TYPE = TypeInfo.of(SoundModifierEntry.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public SoundModifierEntry(Holder<SoundModifier> entry) {
		super(entry);
	}

	public static SoundModifierEntry of(MethodHandles.Lookup caller, String name, Class<?> type, String id, int flags) {
		return of(id, flags);
	}

	public static SoundModifierEntry of(String id, int flags) {
		Holder<SoundModifier> holder = ConstantFactory.getEntry(BigGlobeDynamicRegistries.SOUND_MODIFIER_REGISTRY_KEY, id, flags);
		return holder != null ? new SoundModifierEntry(holder) : null;
	}

	@Override
	public boolean isIn(SoundModifierTag tag) {
		return super.isIn(tag);
	}
}