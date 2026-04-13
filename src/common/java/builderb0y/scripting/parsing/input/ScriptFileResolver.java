package builderb0y.scripting.parsing.input;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.data.Data;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.autocodec.data.ListData;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.EmptyDependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.util.BetterScopedValue;
import builderb0y.bigglobe.util.FakeRegistry;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.util.ArrayBuilder;

public class ScriptFileResolver {

	public static final BetterScopedValue<Map<Identifier, String>> OVERRIDES = new BetterScopedValue<>();
	public static final FakeRegistry<ResolvedInclude> RESOLVED_INCLUDE_REGISTRY = new FakeRegistry<>(ResolvedInclude.REGISTRY_KEY);

	public static Holder<ResolvedInclude> intern(ResolvedInclude include) {
		return RESOLVED_INCLUDE_REGISTRY.getOrCreate(include.id(), include);
	}

	public static ResolvedInclude resolve(Identifier identifier) {
		if (identifier.getNamespace().contains("..") || identifier.getPath().contains("..")) {
			throw new IllegalArgumentException("No, you may not access parent directories this way.");
		}
		Map<Identifier, String> overrides = OVERRIDES.currentValue();
		if (overrides != null) {
			String source = overrides.get(identifier);
			if (source != null) return new ResolvedInclude(identifier, source);
			else throw new IllegalStateException("Missing include " + identifier);
		}
		Identifier full = IdentifierVersions.create(identifier.getNamespace(), "bigglobe/script_file/" + identifier.getPath() + ".gs");
		try (BufferedReader reader = BigGlobeMod.getResourceManager().openAsReader(full)) {
			StringWriter writer = new StringWriter(1024);
			reader.transferTo(writer);
			return new ResolvedInclude(identifier, writer.toString());
		}
		catch (Exception exception) {
			throw new RuntimeException("Failed to read " + full, exception);
		}
	}

	@SuppressWarnings("unchecked") //generic array.
	public static ResolvedIncludes resolveIncludes(Identifier[] includes) {
		if (includes == null) return null;
		return new ResolvedIncludes(
			Arrays
			.stream(includes)
			.map(ScriptFileResolver::resolve)
			.map(ScriptFileResolver::intern)
			.toArray(Holder[]::new)
		);
	}

	public static record ResolvedInclude(Identifier id, String source) implements EmptyDependencyView {

		public static final ResourceKey<Registry<ResolvedInclude>> REGISTRY_KEY = ResourceKey.createRegistryKey(BigGlobeMod.modID("include"));
		public static final ObjectArrayFactory<Holder<ResolvedInclude>> ENTRY_ARRAY_FACTORY = new ObjectArrayFactory<>(Holder.class).generic();

		public void appendTo(StringBuilder builder) {
			builder.append(";BEGIN INCLUDE ").append(this.id).append('\n').append(this.source);
		}
	}

	@UseCoder(name = "new", in = ResolvedIncludesCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
	public static record ResolvedIncludes(Holder<ResolvedInclude>[] includes) implements SimpleDependencyView {

		public String assemble(String source) {
			StringBuilder builder = new StringBuilder(this.includes.length << 9);
			for (Holder<ResolvedInclude> include : this.includes) {
				include.value().appendTo(builder);
				builder.append("\n\n");
			}
			return builder.append(";BEGIN SCRIPT\n").append(source).toString();
		}

		@Override
		public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
			return Arrays.stream(this.includes);
		}
	}

	public static class ResolvedIncludesCoder extends NamedCoder<ResolvedIncludes> {

		public final AutoCoder<Identifier> identifierCoder;

		public ResolvedIncludesCoder(FactoryContext<ResolvedIncludes> context) {
			super(context.type);
			this.identifierCoder = context.type(ReifiedType.from(Identifier.class)).forceCreateCoder();
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @Nullable ResolvedIncludes decode(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
			if (context.isEmpty()) return null;
			ArrayBuilder<Holder<ResolvedInclude>> builder = new ArrayBuilder<>();
			for (DecodeContext<T_Encoded> elementContext : context.listIterable()) {
				builder.accept(intern(resolve(elementContext.decodeWith(this.identifierCoder))));
			}
			return new ResolvedIncludes(builder.toArray(ResolvedInclude.ENTRY_ARRAY_FACTORY));
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull Data encode(@NotNull EncodeContext<T_Encoded, ResolvedIncludes> context) throws EncodeException {
			ResolvedIncludes includes = context.object;
			if (includes == null) return EmptyData.INSTANCE;
			return ListData.collect(
				Arrays
					.stream(includes.includes)
					.map(Holder<ResolvedInclude>::value)
					.map(ResolvedInclude::id)
					.map((Identifier id) -> (
						context
							.object(id)
							.encodeWith(this.identifierCoder)
					))
			);
		}
	}
}