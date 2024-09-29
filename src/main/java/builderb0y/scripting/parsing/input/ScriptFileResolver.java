package builderb0y.scripting.parsing.input;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.FactoryContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.encoders.EncodeContext;
import builderb0y.autocodec.encoders.EncodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.EmptyDependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.versions.IdentifierVersions;

public class ScriptFileResolver {

	public static final ThreadLocal<Map<Identifier, String>> OVERRIDES = new ThreadLocal<>();
	public static final ReferenceQueue<RegistryEntry<ResolvedInclude>> QUEUE = new ReferenceQueue<>();
	public static final ConcurrentHashMap<ResolvedInclude, WeakReference<RegistryEntry<ResolvedInclude>>> CACHE = new ConcurrentHashMap<>(32);

	public static RegistryEntry<ResolvedInclude> intern(ResolvedInclude include) {
		class RegistryEntryImpl extends RegistryEntry.Reference<ResolvedInclude> {

			public RegistryEntryImpl(ResolvedInclude include) {
				super(
					RegistryEntry.Reference.Type.STAND_ALONE,
					ResolvedInclude.OWNER,
					RegistryKey.of(
						ResolvedInclude.REGISTRY_KEY,
						include.id()
					),
					include
				);
			}
		}
		class Ref extends WeakReference<RegistryEntry<ResolvedInclude>> {

			public final ResolvedInclude key;

			public Ref(RegistryEntry<ResolvedInclude> entry) {
				super(entry, QUEUE);
				this.key = entry.value();
			}
		}
		for (Reference<? extends RegistryEntry<ResolvedInclude>> reference; (reference = QUEUE.poll()) != null;) {
			CACHE.remove(((Ref)(reference)).key);
		}
		MutableObject<RegistryEntry<ResolvedInclude>> result = new MutableObject<>();
		CACHE.compute(include, (ResolvedInclude include_, WeakReference<RegistryEntry<ResolvedInclude>> ref) -> {
			RegistryEntry<ResolvedInclude> entry;
			if (ref != null) {
				entry = ref.get();
				if (entry != null) {
					result.setValue(entry);
					return ref;
				}
			}
			entry = new RegistryEntryImpl(include_);
			result.setValue(entry);
			return new Ref(entry);
		});
		return result.getValue();
	}

	public static ResolvedInclude resolve(Identifier identifier) {
		if (identifier.getNamespace().contains("..") || identifier.getPath().contains("..")) {
			throw new IllegalArgumentException("No, you may not access parent directories this way.");
		}
		Map<Identifier, String> overrides = OVERRIDES.get();
		if (overrides != null) {
			String source = overrides.get(identifier);
			if (source != null) return new ResolvedInclude(identifier, source);
			else throw new IllegalStateException("Missing include " + identifier);
		}
		Identifier full = IdentifierVersions.create(identifier.getNamespace(), "bigglobe_script_files/" + identifier.getPath() + ".gs");
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
			.toArray(RegistryEntry[]::new)
		);
	}

	public static record ResolvedInclude(Identifier id, String source) implements EmptyDependencyView {

		public static final RegistryEntryOwner<ResolvedInclude> OWNER = new RegistryEntryOwner<>() {};
		public static final RegistryKey<Registry<ResolvedInclude>> REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.modID("include"));
		public static final ObjectArrayFactory<RegistryEntry<ResolvedInclude>> ENTRY_ARRAY_FACTORY = new ObjectArrayFactory<>(RegistryEntry.class).generic();

		public void appendTo(StringBuilder builder) {
			builder.append(";BEGIN INCLUDE ").append(this.id).append('\n').append(this.source);
		}
	}

	@UseCoder(name = "new", in = ResolvedIncludesCoder.class, usage = MemberUsage.METHOD_IS_FACTORY)
	public static record ResolvedIncludes(RegistryEntry<ResolvedInclude>[] includes) implements SimpleDependencyView {

		public String assemble(String source) {
			StringBuilder builder = new StringBuilder(this.includes.length << 9);
			for (RegistryEntry<ResolvedInclude> include : this.includes) {
				include.value().appendTo(builder);
				builder.append("\n\n");
			}
			return builder.append(";BEGIN SCRIPT\n").append(source).toString();
		}

		@Override
		public Stream<? extends RegistryEntry<? extends DependencyView>> streamDirectDependencies() {
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
			return new ResolvedIncludes(
				context
				.forceAsStream(true)
				.map((DecodeContext<T_Encoded> elementContext) -> {
					try {
						return intern(resolve(elementContext.decodeWith(this.identifierCoder)));
					}
					catch (DecodeException exception) {
						throw AutoCodecUtil.rethrow(exception);
					}
				})
				.toArray(ResolvedInclude.ENTRY_ARRAY_FACTORY)
			);
		}

		@Override
		@OverrideOnly
		public <T_Encoded> @NotNull T_Encoded encode(@NotNull EncodeContext<T_Encoded, ResolvedIncludes> context) throws EncodeException {
			ResolvedIncludes includes = context.object;
			if (includes == null) return context.empty();
			return context.createList(
				Arrays
				.stream(includes.includes)
				.map(RegistryEntry<ResolvedInclude>::value)
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