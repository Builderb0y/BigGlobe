package builderb0y.bigglobe.versions;

import net.minecraft.nbt.NbtString;

public class NbtVersions {

	public static String stringValue(NbtString nbt) {
		#if MC_VERSION >= MC_1_21_5
			return nbt.value();
		#else
			return nbt.asString();
		#endif
	}
}