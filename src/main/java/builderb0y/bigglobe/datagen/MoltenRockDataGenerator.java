package builderb0y.bigglobe.datagen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MoltenRockDataGenerator {

	public static final File
		BASE_PATH = new File("src/main/resources/wip/molten_rock");

	public static void main(String[] args) throws IOException {
		for (int heat = 1; heat <= 8; heat++) {
			String name = "molten_rock_" + ((char)(heat + '0'));
			writeBlockState(name);
			writeBlockModel(name);
			writeItemModel(name);
			writeLootTable(name);
		}
	}

	public static void writeBlockModel(String fileName) throws IOException {
		File file = new File(BASE_PATH, "blockModels" + File.separatorChar + fileName + ".json");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(
				"""
				{
					"parent": "minecraft:block/cube_all",
					"textures": {
						"all": "bigglobe:block/%NAME"
					}
				}""".replace("%NAME", fileName)
			);
		}
	}

	public static void writeItemModel(String fileName) throws IOException {
		File file = new File(BASE_PATH, "itemModels" + File.separatorChar + fileName + ".json");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(
				"""
				{
					"parent": "bigglobe:block/%NAME"
				}""".replace("%NAME", fileName)
			);
		}
	}

	public static void writeBlockState(String fileName) throws IOException {
		File file = new File(BASE_PATH, "blockstates" + File.separatorChar + fileName + ".json");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(
				"""
				{
					"variants": {
						"": { "model": "bigglobe:block/%NAME" }
					}
				}""".replace("%NAME", fileName)
			);
		}
	}

	public static void writeLootTable(String fileName) throws IOException {
		File file = new File(BASE_PATH, "lootTables" + File.separatorChar + fileName + ".json");
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(
				"""
				{
					"type": "minecraft:block"
				}"""
			);
		}
	}
}