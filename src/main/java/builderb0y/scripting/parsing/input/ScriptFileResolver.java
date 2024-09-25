package builderb0y.scripting.parsing.input;

import java.io.BufferedReader;
import java.io.StringWriter;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.versions.IdentifierVersions;

public class ScriptFileResolver {

	public static String resolve(Identifier identifier) {
		if (identifier.getNamespace().contains("..") || identifier.getPath().contains("..")) {
			throw new IllegalArgumentException("No, you may not access parent directories this way.");
		}
		Identifier full = IdentifierVersions.create(identifier.getNamespace(), "bigglobe_script_files/" + identifier.getPath() + ".gs");
		try (BufferedReader reader = BigGlobeMod.getResourceFactory().openAsReader(full)) {
			StringWriter writer = new StringWriter(1024);
			reader.transferTo(writer);
			return writer.toString();
		}
		catch (Exception exception) {
			throw new RuntimeException("Failed to read " + full, exception);
		}
	}

	public static String resolveIncludes(Identifier[] includes) {
		if (includes == null) return null;
		StringBuilder builder = new StringBuilder(8192);
		for (Identifier include : includes) {
			builder
			.append(";BEGIN INCLUDE ")
			.append(include)
			.append('\n')
			.append(resolve(include))
			.append("\n\n");
		}
		return builder.append(";BEGIN SCRIPT\n").toString();
	}
}