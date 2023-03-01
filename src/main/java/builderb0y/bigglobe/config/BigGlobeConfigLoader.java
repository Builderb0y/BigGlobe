package builderb0y.bigglobe.config;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.primitives.Primitives;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.nbt.NbtElement;

import builderb0y.autocodec.AutoCodec;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.logging.StackContextLogger;
import builderb0y.autocodec.logging.TaskLogger;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class BigGlobeConfigLoader {

	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Config");
	public static final AutoCodec AUTO_CODEC = new AutoCodec() {

		@Override
		public @NotNull TaskLogger createDefaultLogger(@NotNull ReentrantLock lock) {
			return new StackContextLogger(lock, BigGlobeAutoCodec.createPrinter(LOGGER), true);
		}
	};
	public static final String CONFIG_FILE_NAME = BigGlobeMod.MODNAME + ".json5";
	public static final Path
		CONFIG_FOLDER         = FabricLoader.getInstance().getConfigDir().toAbsolutePath().resolve(BigGlobeMod.MODID),
		CONFIG_FILE           = CONFIG_FOLDER.resolve(CONFIG_FILE_NAME),
		TEMPORARY_CONFIG_FILE = CONFIG_FOLDER.resolve(CONFIG_FILE_NAME + ".tmp");

	public static BigGlobeConfig load() throws Exception {
		if (Files.exists(CONFIG_FILE)) {
			String text = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
			JsonElement json = JsonParser.parseString(text);
			return AUTO_CODEC.decode(AUTO_CODEC.createDecoder(BigGlobeConfig.class), json, JsonOps.INSTANCE);
		}
		else {
			return new BigGlobeConfig();
		}
	}

	public static void save(BigGlobeConfig config) throws Exception {
		Files.createDirectories(CONFIG_FOLDER);
		Files.writeString(TEMPORARY_CONFIG_FILE, toString(config), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
		Files.move(TEMPORARY_CONFIG_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
	}

	public static BigGlobeConfig loadAndSave() {
		String oldText = null;
		BigGlobeConfig config = null;
		if (Files.exists(CONFIG_FILE)) try {
			oldText = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
			JsonElement json = JsonParser.parseString(oldText);
			config = AUTO_CODEC.decode(AUTO_CODEC.createDecoder(BigGlobeConfig.class), json, JsonOps.INSTANCE);
			config.validatePostLoad();
		}
		catch (Exception exceptionParsing) {
			LOGGER.error("Could not parse " + CONFIG_FILE, exceptionParsing);
			config = new BigGlobeConfig();
		}
		else {
			config = new BigGlobeConfig();
		}

		try {
			String newText = toString(config);
			if (!newText.equals(oldText)) {
				Files.createDirectories(CONFIG_FOLDER);
				Files.writeString(TEMPORARY_CONFIG_FILE, newText, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				Files.move(TEMPORARY_CONFIG_FILE, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (Exception exception) {
			LOGGER.error("Could not save " + CONFIG_FILE, exception);
		}
		return config;
	}

	public static String toString(BigGlobeConfig config) throws Exception {
		JsonObject lang;
		try (InputStream stream = BigGlobeMod.class.getResourceAsStream("/assets/bigglobe/lang/en_us.json")) {
			if (stream == null) throw new FileNotFoundException("/assets/bigglobe/lang/en_us.json");
			lang = (JsonObject)(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
		}
		return toStringBuilder(new StringBuilder(3072), config, 0, "text.autoconfig.bigglobe.option", lang).toString();
	}

	/**
	{@link AutoCoder}'s are nice and all, but neither {@link JsonElement}
	nor {@link NbtElement} supports comments. in order to support comments,
	I would *either* need to implement a 3rd intermediate storage,
	or just do the serializing manually. I opted for the latter.
	*/
	public static StringBuilder toStringBuilder(StringBuilder builder, Object object, int depth, String path, JsonObject lang) throws IllegalAccessException {
		if (Primitives.isWrapperType(object.getClass())) {
			return builder.append(object);
		}
		else if (object instanceof String s) {
			return appendString(builder, s);
		}
		else {
			builder.append('{');
			depth++;
			for (Field field : object.getClass().getFields()) {
				if (Modifier.isStatic(field.getModifiers())) continue;
				String nextPath = path + '.' + field.getName();
				builder.append('\n');
				for (int index = 0; true; index++) {
					JsonPrimitive line = lang.getAsJsonPrimitive(nextPath + ".@Tooltip[" + index + ']');
					if (line == null) break;
					tab(builder, depth).append("//").append(line.getAsString());
				}
				UseName useName = field.getAnnotatedType().getDeclaredAnnotation(UseName.class);
				String name = useName != null ? useName.value() : field.getName();
				appendString(tab(builder, depth), name).append(": ");
				toStringBuilder(builder, field.get(object), depth, nextPath, lang).append(',');
			}
			builder.setLength(builder.length() - 1); //delete trailing comma.
			depth--;
			return tab(builder, depth).append('}');
		}
	}

	public static StringBuilder tab(StringBuilder out, int indentation) {
		out.ensureCapacity(out.length() + indentation + 1);
		out.append('\n');
		for (int i = 0; i < indentation; i++) {
			out.append('\t');
		}
		return out;
	}

	public static StringBuilder appendString(StringBuilder out, String text) {
		out.ensureCapacity(out.length() + text.length() + 8);
		out.append('"');
		for (int index = 0, length = text.length(); index < length; index++) {
			char c = text.charAt(index);
			switch (c) {
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				case '\f' -> out.append("\\f");
				case '\b' -> out.append("\\b");
				case '"'  -> out.append("\\\"");
				case '\\' -> out.append("\\\\");
				default -> {
					if (c < ' ' || c > '~') {
						out
						.append("\\u")
						.append(Character.toUpperCase(Character.forDigit((c >>> 12) & 0xF, 16)))
						.append(Character.toUpperCase(Character.forDigit((c >>>  8) & 0xF, 16)))
						.append(Character.toUpperCase(Character.forDigit((c >>>  4) & 0xF, 16)))
						.append(Character.toUpperCase(Character.forDigit((c       ) & 0xF, 16)));
					}
					else {
						out.append(c);
					}
				}
			}
		}
		return out.append('"');
	}
}