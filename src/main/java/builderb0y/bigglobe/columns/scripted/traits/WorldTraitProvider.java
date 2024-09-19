package builderb0y.bigglobe.columns.scripted.traits;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.*;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.parsing.input.SourceScriptUsage;

@UseCoder(name = "new", in = WorldTraitProvider.Coder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public record WorldTraitProvider(ScriptUsage get, @VerifyNullable ScriptUsage set) {

	public WorldTraitProvider(ScriptUsage get) {
		this(get, null);
	}

	public WorldTraitProvider(String get) {
		this(new SourceScriptUsage(get), null);
	}

	public static class Coder extends NamedCoder<WorldTraitProvider> {

		public final AutoCoder<ScriptUsage> scriptUsageCoder;

		public Coder(@NotNull FactoryContext<WorldTraitProvider> context) {
			super(context.type);
			this.scriptUsageCoder = context.type(ReifiedType.from(ScriptUsage.class)).forceCreateCoder();
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable WorldTraitProvider decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			if (context.isMap()) {
				DecodeContext<T_Encoded> getter = context.getMember("get");
				if (!getter.isEmpty()) {
					DecodeContext<T_Encoded> setter = context.getMember("set");
					if (!setter.isEmpty()) {
						return new WorldTraitProvider(getter.decodeWith(this.scriptUsageCoder), setter.decodeWith(this.scriptUsageCoder));
					}
					else {
						return new WorldTraitProvider(getter.decodeWith(this.scriptUsageCoder));
					}
				}
			}
			return new WorldTraitProvider(context.decodeWith(this.scriptUsageCoder));
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, WorldTraitProvider> context) throws EncodeException {
			WorldTraitProvider provider = context.object;
			if (provider == null) return context.empty();
			if (provider.set == null) {
				return context.object(provider.get).encodeWith(this.scriptUsageCoder);
			}
			else {
				return context.createStringMap(
					Map.of(
						"get", context.object(provider.get).encodeWith(this.scriptUsageCoder),
						"set", context.object(provider.set).encodeWith(this.scriptUsageCoder)
					)
				);
			}
		}
	}
}