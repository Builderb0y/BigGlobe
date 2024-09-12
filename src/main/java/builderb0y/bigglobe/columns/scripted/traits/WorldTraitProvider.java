package builderb0y.bigglobe.columns.scripted.traits;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseDecoder;
import builderb0y.autocodec.annotations.UseEncoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.AutoDecoder.NamedDecoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.AutoEncoder;
import builderb0y.autocodec.encoders.AutoEncoder.NamedEncoder;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.encoders.MultiFieldEncoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.scripting.parsing.ScriptUsage;

@UseDecoder(name = "new", in = WorldTraitProvider.Decoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
@UseEncoder(name = "new", in = WorldTraitProvider.Encoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
public record WorldTraitProvider(ScriptUsage get, @VerifyNullable ScriptUsage set) {

	public WorldTraitProvider(ScriptUsage get) {
		this(get, null);
	}

	public WorldTraitProvider(String get) {
		this(new ScriptUsage(get), null);
	}

	public static class Decoder extends NamedDecoder<WorldTraitProvider> {

		public static final AutoCoder<ScriptUsage> SCRIPT_USAGE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptUsage.class);

		public Decoder(FactoryContext<WorldTraitProvider> context) {
			super(context.type);
		}

		@Override
		public <T_Encoded> @Nullable WorldTraitProvider decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			if (context.isMap()) {
				DecodeContext<T_Encoded> getter = context.getMember("get");
				if (!getter.isEmpty()) {
					DecodeContext<T_Encoded> setter = context.getMember("set");
					if (!setter.isEmpty()) {
						return new WorldTraitProvider(getter.decodeWith(SCRIPT_USAGE_CODER), setter.decodeWith(SCRIPT_USAGE_CODER));
					}
					else {
						return new WorldTraitProvider(getter.decodeWith(SCRIPT_USAGE_CODER));
					}
				}
			}
			return new WorldTraitProvider(context.decodeWith(SCRIPT_USAGE_CODER));
		}
	}

	public static class Encoder extends NamedEncoder<WorldTraitProvider> {

		public final AutoEncoder<WorldTraitProvider> fallback;

		public Encoder(FactoryContext<WorldTraitProvider> context) {
			super(context.type);
			this.fallback = context.type(ReifiedType.from(WorldTraitProvider.class).addAnnotation(VerifyNullable.INSTANCE)).forceCreateEncoder(MultiFieldEncoder.Factory.INSTANCE);
		}

		@Override
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, WorldTraitProvider> context) throws EncodeException {
			WorldTraitProvider provider = context.input;
			if (provider == null) return context.empty();
			if (provider.set == null) {
				return context.input(provider.get).encodeWith(Decoder.SCRIPT_USAGE_CODER);
			}
			else {
				return context.createStringMap(
					Map.of(
						"get", context.input(provider.get).encodeWith(Decoder.SCRIPT_USAGE_CODER),
						"set", context.input(provider.set).encodeWith(Decoder.SCRIPT_USAGE_CODER)
					)
				);
			}
		}
	}
}