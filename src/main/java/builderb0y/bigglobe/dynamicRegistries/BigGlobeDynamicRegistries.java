package builderb0y.bigglobe.dynamicRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.*;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.randomLists.ConstantContainedRandomList;
import builderb0y.bigglobe.randomLists.IRandomList;
import builderb0y.bigglobe.randomLists.IWeightedListElement;
import builderb0y.bigglobe.settings.BiomeLayout.EndBiomeLayout;
import builderb0y.bigglobe.settings.BiomeLayout.OverworldBiomeLayout;
import builderb0y.bigglobe.settings.NetherSettings.LocalNetherSettings;
import builderb0y.bigglobe.settings.OverworldCaveSettings.LocalOverworldCaveSettings;
import builderb0y.bigglobe.settings.OverworldCavernSettings.LocalCavernSettings;
import builderb0y.bigglobe.settings.OverworldSkylandSettings.LocalSkylandSettings;
import builderb0y.bigglobe.structures.scripted.StructurePlacementScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.parsing.ScriptTemplate;

public class BigGlobeDynamicRegistries {

	public static final RegistryKey<Registry<ScriptTemplate>>                  SCRIPT_TEMPLATE_REGISTRY_KEY                 = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_script_templates"));
	public static final RegistryKey<Registry<StructurePlacementScript.Holder>> SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY      = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_script_structure_placement"));
	public static final RegistryKey<Registry<WoodPalette>>                     WOOD_PALETTE_REGISTRY_KEY                    = RegistryKey.ofRegistry(BigGlobeMod.mcID("bigglobe_wood_palettes"));
	public static final RegistryKey<Registry<OverworldBiomeLayout>>            OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_biome_layout"));
	public static final RegistryKey<Registry<LocalOverworldCaveSettings>>      LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY   = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caves"));
	public static final RegistryKey<Registry<LocalCavernSettings>>             LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_overworld_caverns"));
	public static final RegistryKey<Registry<LocalSkylandSettings>>            LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY          = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_skylands"));
	public static final RegistryKey<Registry<LocalNetherSettings>>             LOCAL_NETHER_SETTINGS_REGISTRY_KEY           = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_nether_biome"));
	public static final RegistryKey<Registry<EndBiomeLayout>>                  END_BIOME_LAYOUT_REGISTRY_KEY                = RegistryKey.ofRegistry(BigGlobeMod.mcID("worldgen/bigglobe_end_biome_layout"));

	public static final List<DynamicRegistryManager.Info<?>> INFOS = new ArrayList<>(9);
	static {
		INFOS.add(info(SCRIPT_TEMPLATE_REGISTRY_KEY,                 ScriptTemplate                 .class, null));
		INFOS.add(info(SCRIPT_STRUCTURE_PLACEMENT_REGISTRY_KEY,      StructurePlacementScript.Holder.class, null));
		INFOS.add(info(WOOD_PALETTE_REGISTRY_KEY,                    WoodPalette                    .class, null));
		INFOS.add(info(OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY,          OverworldBiomeLayout           .class, null));
		INFOS.add(info(END_BIOME_LAYOUT_REGISTRY_KEY,                EndBiomeLayout                 .class, null));
		INFOS.add(info(LOCAL_OVERWORLD_CAVE_SETTINGS_REGISTRY_KEY,   LocalOverworldCaveSettings     .class, null));
		INFOS.add(info(LOCAL_OVERWORLD_CAVERN_SETTINGS_REGISTRY_KEY, LocalCavernSettings            .class, null));
		INFOS.add(info(LOCAL_SKYLAND_SETTINGS_REGISTRY_KEY,          LocalSkylandSettings           .class, null));
		INFOS.add(info(LOCAL_NETHER_SETTINGS_REGISTRY_KEY,           LocalNetherSettings            .class, null));
	}

	public static <E> DynamicRegistryManager.Info<E> info(RegistryKey<Registry<E>> key, Class<E> clazz) {
		Codec<E> codec = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(clazz);
		return new DynamicRegistryManager.Info<>(key, codec, codec);
	}

	public static <E> DynamicRegistryManager.Info<E> info(RegistryKey<Registry<E>> key, Class<E> clazz, Codec<E> networkCodec) {
		return new DynamicRegistryManager.Info<>(
			key,
			BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(clazz),
			networkCodec
		);
	}

	public static void registerBigGlobeDynamicRegistries(ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> builder) {
		for (DynamicRegistryManager.Info<?> info : INFOS) {
			builder.put(info.registry(), info);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void addBuiltin() {
		BigGlobeMod.LOGGER.debug("Adding " + BigGlobeMod.MODNAME + " objects to builtin registries...");
		for (DynamicRegistryManager.Info<?> info : INFOS) {
			((MutableRegistry)(BuiltinRegistries.REGISTRIES)).add(info.registry(), new SimpleRegistry<>(info.registry(), Lifecycle.experimental(), null), Lifecycle.experimental());
		}
		/*
		DynamicRegistryManager dynamicRegistryManager = DynamicRegistryManager.of(BuiltinRegistries.REGISTRIES);
		RegistryOps<JsonElement> registryOps = RegistryOps.of(JsonOps.INSTANCE, dynamicRegistryManager);
		try (BuiltInLoader loader = BuiltInLoader.get(registryOps)) {
			for (DynamicRegistryManager.Info<?> info : DynamicRegistryManager.INFOS.values()) {
				loader.load(((Registry)(((Registry)(BuiltinRegistries.REGISTRIES)).get(info.registry()))), BigGlobeAutoCodec.AUTO_CODEC.wrapDFUCodec(info.entryCodec(), false));
			}
		}
		catch (Exception exception) {
			BigGlobeMod.LOGGER.error("Failed to load Big Globe dynamic registry objects!", exception);
			throw new RuntimeException(exception);
		}
		*/
		BigGlobeMod.LOGGER.debug("Done adding " + BigGlobeMod.MODNAME + " objects to builtin registries.");
	}

	public static <T extends IWeightedListElement> IRandomList<T> sortAndCollect(Registry<T> registry) {
		ConstantContainedRandomList<T> list = new ConstantContainedRandomList<>();
		registry
		.streamEntries()
		.sorted(
			Comparator.comparing(
				(RegistryEntry<T> entry) -> (
					UnregisteredObjectException.getKey(entry).getValue()
				),
				Comparator
				.comparing(Identifier::getNamespace)
				.thenComparing(Identifier::getPath)
			)
		)
		.map(RegistryEntry::value)
		.forEachOrdered(list::add);
		if (list.isEmpty()) throw new IllegalStateException(registry.getKey().getValue() + " is empty");
		return list;
	}

	/**
	UNUSED: I got errors when I didn't add my registries to {@link BuiltinRegistries#ROOT},
	so my assumption was that I needed to add them and fill them with elements.
	but it turns out the filling with elements part was not necessary.
	as such, this class and its subclasses are left here,
	but commented out in case I need something similar in the future.

	a class designed to read the contents of the Big Globe.jar file
	looking for json files to register to Big Globe's dynamic registries.
	there are 2 subclasses of BuiltInLoader:

	{@link JarLoader} will be used in production when Big Globe.jar is an actual jar file.

	{@link DirectoryLoader} will be used in dev environments, when Big Globe.jar
	does not actually exist, and classes are loaded from intellij's "out" directory.
	*/
	/*
	public static abstract class BuiltInLoader implements Closeable {

		public static final String[] NAMESPACES = { BigGlobeMod.MODID, "minecraft" };

		public final RegistryOps<JsonElement> registryOps;

		public BuiltInLoader(RegistryOps<JsonElement> registryOps) {
			this.registryOps = registryOps;
		}

		public static BuiltInLoader get(RegistryOps<JsonElement> registryOps) throws IOException, URISyntaxException {
			File jarFile = new File(BigGlobeMod.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			if (jarFile.isFile()) {
				BigGlobeMod.LOGGER.debug(BigGlobeMod.MODNAME + ".jar is a FILE.");
				return new JarLoader(registryOps, new JarFile(jarFile));
			}
			else if (jarFile.isDirectory()) {
				BigGlobeMod.LOGGER.debug(BigGlobeMod.MODNAME + ".jar is a DIRECTORY.");
				return new DirectoryLoader(registryOps, new File(jarFile, "data"));
			}
			else {
				throw new UnsupportedOperationException("Big Globe.jar is neither a regular file nor a directory.");
			}
		}

		public abstract <T> void load(Registry<T> registry, AutoDecoder<T> decoder) throws IOException, DecodeException;

		public <T> void load(Registry<T> registry, AutoDecoder<T> decoder, String namespace, String path, InputStream stream) throws IOException, DecodeException {
			try (Reader reader = new InputStreamReader(new BufferedInputStream(stream), StandardCharsets.UTF_8)) {
				JsonElement json = JsonParser.parseReader(reader);
				T object = BigGlobeAutoCodec.AUTO_CODEC.decode(decoder, json, this.registryOps);
				Registry.register(registry, new Identifier(namespace, path), object);
			}
			System.out.println("Loaded " + registry.getKey().getValue() + " / " + namespace + ':' + path);
		}
	}

	public static class JarLoader extends BuiltInLoader {

		public final JarFile jarFile;

		public JarLoader(RegistryOps<JsonElement> registryOps, JarFile jarFile) {
			super(registryOps);
			this.jarFile = jarFile;
		}

		@Override
		public <T> void load(Registry<T> registry, AutoDecoder<T> decoder) throws IOException, DecodeException {
			record NamespacePrefix(String namespace, String prefix) {}
			NamespacePrefix[] prefixes = (
				Arrays.stream(NAMESPACES)
				.map(namespace -> new NamespacePrefix(
					namespace,
					"data/" + namespace + '/' + registry.getKey().getValue().getPath() + '/'
				))
				.toArray(NamespacePrefix[]::new)
			);
			for (Enumeration<JarEntry> jarEntries = this.jarFile.entries(); jarEntries.hasMoreElements();) {
				JarEntry jarEntry = jarEntries.nextElement();
				for (NamespacePrefix namespacePrefix : prefixes) {
					if (jarEntry.getName().startsWith(namespacePrefix.prefix) && jarEntry.getName().endsWith(".json")) {
						String resourcePath = jarEntry.getName().substring(namespacePrefix.prefix.length(), jarEntry.getName().length() - ".json".length());
						this.load(registry, decoder, namespacePrefix.namespace, resourcePath, this.jarFile.getInputStream(jarEntry));
					}
				}
			}
		}

		@Override
		public void close() throws IOException {
			this.jarFile.close();
		}
	}

	public static class DirectoryLoader extends BuiltInLoader {

		public final File dataDirectory;

		public DirectoryLoader(RegistryOps<JsonElement> registryOps, File dataDirectory) {
			super(registryOps);
			this.dataDirectory = dataDirectory;
		}

		@Override
		public <T> void load(Registry<T> registry, AutoDecoder<T> decoder) throws IOException, DecodeException {
			for (String namespace : NAMESPACES) {
				this.loadRecursive(
					registry,
					decoder,
					new File(
						this.dataDirectory,
						namespace + File.separatorChar + registry.getKey().getValue().getPath().replace('/', File.separatorChar)
					),
					namespace,
					null
				);
			}
		}

		public <T> void loadRecursive(Registry<T> registry, AutoDecoder<T> decoder, File file, String namespace, String path) throws IOException, DecodeException {
			String[] names = file.list();
			if (names != null) {
				for (String name : names) {
					File nextFile = new File(file, name);
					String nextPath = path == null ? name : path + '/' + name;
					this.loadRecursive(registry, decoder, nextFile, namespace, nextPath);
				}
			}
			else if (file.getPath().endsWith(".json") && file.isFile()) {
				this.load(registry, decoder, namespace, path.substring(0, path.length() - ".json".length()), new FileInputStream(file));
			}
		}

		@Override
		public void close() throws IOException {}
	}
	*/
}