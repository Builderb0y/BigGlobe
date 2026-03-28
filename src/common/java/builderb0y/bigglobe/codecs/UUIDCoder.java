package builderb0y.bigglobe.codecs;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.IntListData;
import builderb0y.autocodec.data.StringData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;

public class UUIDCoder extends NamedCoder<UUID> {

	public static final UUIDCoder INSTANCE = new UUIDCoder();

	public UUIDCoder() {
		super("UUIDCoder");
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @Nullable UUID decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		if (context.isEmpty()) return null;
		try {
			IntListData intList = context.tryAsIntList();
			if (intList != null && intList.size() == 4) {
				return UUIDUtil.uuidFromIntArray(intList.value.toIntArray());
			}
			StringData string = context.tryAsString();
			if (string != null) {
				return UUID.fromString(string.value);
			}
		}
		catch (RuntimeException exception) {
			throw new DecodeException(exception);
		}
		throw context.notA("int list or string");
	}

	@Override
	@OverrideOnly
	public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, UUID> context) throws EncodeException {
		UUID uuid = context.object;
		if (uuid == null) return EmptyData.INSTANCE;
		return context.isCompressed() ? IntListData.wrap(UUIDUtil.uuidToIntArray(uuid)) : new StringData(uuid.toString());
	}
}