package builderb0y.bigglobe.generators;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.lwjgl.openal.*;

import builderb0y.autocodec.common.Case;
import builderb0y.bigglobe.generators.Table.Justification;

public class SoundEffectsGenerator {

	public static class EffectFields {

		public Field param, default_, min, max;

		public boolean isFull() {
			return (
				this.param != null &&
				this.default_ != null &&
				this.min != null &&
				this.max != null &&
				this.default_.getType() == this.min.getType() &&
				this.min.getType() == this.max.getType()
			);
		}

		public boolean isBoolean() throws IllegalAccessException {
			return this.default_.getType() == int.class && ((int)(this.min.get(null))) == 0 && ((int)(this.max.get(null))) == 1;
		}

		public static String get(String[] parts, int index) {
			return index < parts.length ? parts[index] : null;
		}

		public static String rest(String[] parts, int startIndex) {
			StringJoiner joiner = new StringJoiner("_");
			for (int index = startIndex; index < parts.length; index++) {
				joiner.add(parts[index]);
			}
			return joiner.toString();
		}
	}

	public static class ParamMap extends TreeMap<String, EffectFields> {

		public Field param;

		public boolean isFull() {
			return this.param != null && this.values().stream().anyMatch(EffectFields::isFull);
		}
	}

	enum ModifierType {
		EFFECT,
		FILTER;
	}

	public static void main() throws Throwable{
		Map<String, Field> allFields = Arrays.stream(EXTEfx.class.getDeclaredFields()).collect(Collectors.toMap(Field::getName, Function.identity()));
		Map<String, ParamMap> allParams = new TreeMap<>();
		Map<String, ModifierType> modifierTypes = new TreeMap<>();
		for (Field field : allFields.values()) {
			if ((field.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)) == (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)) {
				String[] parts = field.getName().split("_");
				if ("AL".equals(EffectFields.get(parts, 0))) {
					String first = EffectFields.get(parts, 1);
					switch (first) {
						case null -> {}
						case "EFFECT" -> {
							String rest = EffectFields.rest(parts, 2);
							modifierTypes.put(rest, ModifierType.EFFECT);
							allParams.computeIfAbsent(rest, (String _) -> new ParamMap()).param = field;
						}
						case "FILTER" -> {
							String rest = EffectFields.rest(parts, 2);
							modifierTypes.put(rest, ModifierType.FILTER);
							allParams.computeIfAbsent(rest, (String _) -> new ParamMap()).param = field;
						}
						default -> {
							ParamMap params = allParams.computeIfAbsent(first, (String _) -> new ParamMap());
							switch (EffectFields.get(parts, 2)) {
								case null -> {}
								case "MIN" -> {
									params.computeIfAbsent(EffectFields.rest(parts, 3), (String _) -> new EffectFields()).min = field;
								}
								case "MAX" -> {
									params.computeIfAbsent(EffectFields.rest(parts, 3), (String _) -> new EffectFields()).max = field;
								}
								case "DEFAULT" -> {
									params.computeIfAbsent(EffectFields.rest(parts, 3), (String _) -> new EffectFields()).default_ = field;
								}
								default -> {
									params.computeIfAbsent(EffectFields.rest(parts, 2), (String _) -> new EffectFields()).param = field;
								}
							}
						}
					}
				}
			}
		}
		Table table = new Table(), registration = new Table();
		int registrationIndex = 0;
		for (Map.Entry<String, ModifierType> effectType : modifierTypes.entrySet()) {
			StringBuilder file = new StringBuilder(4096);
			ParamMap params = allParams.get(effectType.getKey());
			if (params != null && params.isFull()) {
				String modifierType = Case.PASCAL_CASE.apply(effectType.getValue().name());
				String className = Case.PASCAL_CASE.apply(effectType.getKey()) + "Sound" + modifierType;
				registration.getCell(0, registrationIndex).append("REGISTRY.registerAuto(BigGlobeMod.modID(");
				registration.getCell(1, registrationIndex).append('"').append(effectType.getKey().toLowerCase(Locale.ROOT)).append('"');
				registration.getCell(2, registrationIndex).append("), ");
				registration.getCell(3, registrationIndex).append(className).justify(Justification.RIGHT);
				registration.getCell(4, registrationIndex).append(".class);");
				registrationIndex++;
				file.append("""
				package builderb0y.bigglobe.sounds;

				import net.fabricmc.api.EnvType;
				import net.fabricmc.api.Environment;
				import org.jetbrains.annotations.Nullable;
				import org.lwjgl.openal.*;

				import builderb0y.autocodec.annotations.*;

				import static org.lwjgl.openal.EXTEfx.*;

				""");
				file.append("public record ").append(className).append("(\n");
				int row = 0;
				for (Map.Entry<String, EffectFields> effectParamEntry : params.entrySet()) {
					EffectFields effectFields = effectParamEntry.getValue();
					if (effectFields.isFull()) {
						Class<?> type = effectFields.default_.getType();
						String typeName = type.getSimpleName();
						String capitalizedTypeName = Case.PASCAL_CASE.apply(typeName);
						String componentName = effectParamEntry.getKey().toLowerCase(Locale.ROOT);
						if (effectFields.isBoolean()) {
							table.getCell(0, row).append("\t@DefaultBoolean");
							table.getCell(1, row).append("(").append(effectFields.default_.getName()).append(" != 0");
							table.getCell(2, row).append(")");
							table.getCell(6, row).append("  boolean ");
							table.getCell(7, row).append(componentName).append(',');
						}
						else {
							table.getCell(0, row).append("\t@Default").append(capitalizedTypeName);
							table.getCell(1, row).append('(').append(effectFields.default_.getName());
							table.getCell(2, row).append(") ");
							table.getCell(3, row).append("@Verify").append(capitalizedTypeName).append("Range");
							table.getCell(4, row).append("(min = ").append(effectFields.min.getName()).append(", ");
							table.getCell(5, row).append("max = ").append(effectFields.max.getName());
							table.getCell(6, row).append(") ").append(typeName).append(' ');
							table.getCell(7, row).append(componentName).append(',');
						}
						row++;
					}
				}
				file.append(table.toStringBuilder());
				file.setLength(file.length() - 1);
				table.clear();

				String fieldName = effectType.getValue().name().toLowerCase(Locale.ROOT);
				file
				.append('\n')
				.append(")\n")
				.append("implements Sound").append(modifierType).append(" {\n")
				.append('\n')
				.append("\t@Override\n")
				.append("\t@Environment(EnvType.CLIENT)\n")
				.append("\tpublic @Nullable AL").append(modifierType).append("Resource createResource() {\n")
				.append("\t\tAL").append(modifierType).append("Resource resource = Sound").append(modifierType).append(".super.createResource();\n")
				.append("\t\tif (resource != null) {\n");

				row = 0;
				table.getCell(0, row).append("\t\t\tal").append(modifierType).append("i(resource.").append(fieldName).append(", ");
				table.getCell(1, row).append("AL_").append(effectType.getValue().name()).append("_TYPE, ");
				table.getCell(2, row).append(params.param.getName());
				table.getCell(3, row).append(");");
				for (Map.Entry<String, EffectFields> effectParamEntry : params.entrySet()) {
					EffectFields effectFields = effectParamEntry.getValue();
					if (effectFields.isFull()) {
						row++;
						char suffix = effectFields.default_.getType().getSimpleName().charAt(0);
						table.getCell(0, row).append("\t\t\tal").append(modifierType).append(suffix).append("(resource.").append(fieldName).append(", ");
						table.getCell(1, row).append(effectFields.param.getName()).append(", ");
						table.getCell(2, row).append("this.").append(effectParamEntry.getKey().toLowerCase(Locale.ROOT));
						if (effectFields.isBoolean()) {
							table.getCell(2, row).append(" ? 1 : 0");
						}
						table.getCell(3, row).append(");");
					}
				}
				file.append(table);
				if (effectType.getValue() == ModifierType.EFFECT) {
					file.append("\n\t\t\talAuxiliaryEffectSloti(resource.slot, AL_EFFECTSLOT_EFFECT, resource.effect);");
				}
				file.append("\n\t\t}\n\t\treturn resource;\n\t}\n}");
				table.clear();
				try (FileWriter writer = new FileWriter("src/common/java/builderb0y/bigglobe/sounds/".replace('/', File.separatorChar) + className + ".java", StandardCharsets.UTF_8)) {
					writer.write(file.toString());
				}
			}
		}
		System.out.println(registration);
	}
}