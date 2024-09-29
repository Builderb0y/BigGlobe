package builderb0y.scripting.parsing.input;

import java.util.Map;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.TypelessCoderRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.input.ScriptFileResolver.ResolvedIncludes;

@UseCoder(name = "CODER", in = ScriptUsage.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class ScriptUsage implements SimpleDependencyView {

	public static final TypelessCoderRegistry<ScriptUsage> CODER = new TypelessCoderRegistry<>(ReifiedType.from(ScriptUsage.class), BigGlobeAutoCodec.AUTO_CODEC) {

		public final AutoCoder<@MultiLine String> sourceCoder = this.autoCodec.createCoder(new ReifiedType<@MultiLine String>() {});

		@Override
		@OverrideOnly
		public @Nullable <T_Encoded> ScriptUsage decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isString() || context.isList()) {
				return new SourceScriptUsage(context.decodeWith(this.sourceCoder));
			}
			return super.decode(context);
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ScriptUsage> context) throws EncodeException {
			if (context.object instanceof SourceScriptUsage source && source.debug_name == null) {
				return context.object(source.source).encodeWith(this.sourceCoder);
			}
			return super.encode(context);
		}
	};
	static {
		CODER.register(SourceScriptUsage.class);
		CODER.register(FileScriptUsage.class);
		CODER.register(TemplateScriptUsage.class);
	}

	public final @VerifyNullable @IdentifierName String debug_name;
	public final @VerifyNullable ResolvedIncludes includes;

	public ScriptUsage(@VerifyNullable @IdentifierName String debug_name, @Nullable ResolvedIncludes includes) {
		this.debug_name = debug_name;
		this.includes = includes;
	}

	@Override
	public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
		if (this.includes != null) {
			if (this.getTemplate() != null) {
				return Stream.concat(this.includes.streamDirectDependencies(), Stream.of(this.getTemplate()));
			}
			else {
				return this.includes.streamDirectDependencies();
			}
		}
		else {
			if (this.getTemplate() != null) {
				return Stream.of(this.getTemplate());
			}
			else {
				return Stream.empty();
			}
		}
	}

	public abstract String getRawSource();

	public String getSource() {
		return this.includes != null ? this.includes.assemble(this.getRawSource()) : this.getRawSource();
	}

	public String getDebugName() {
		return this.debug_name;
	}

	public @Nullable Identifier getFile() {
		return null;
	}

	public @Nullable RegistryEntry<ScriptTemplate> getTemplate() {
		return null;
	}

	public @Nullable Map<@IdentifierName String, @MultiLine String> getInputs() {
		return null;
	}
}