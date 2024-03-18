package builderb0y.bigglobe.texturegen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import builderb0y.bigglobe.noise.Permuter;

import static builderb0y.bigglobe.math.BigGlobeMath.*;
import static java.lang.Math.*;

public class CloudDataGenerator {

	public static final double
		TAU = PI * 2.0D;
	public static final int
		wrap = 4,
		resolution = 16,
		frames = 20 * 5;
	public static final long
		SEED = 0xE1A8712CD981E374L;
	public static final File
		BASE_PATH = new File("src/main/resources/wip/clouds");

	public static void main(String[] args) throws IOException {
		BASE_PATH.mkdirs();
		for (CloudColor color : CloudColor.VALUES) {
			generateImage(color, false);
			generateImage(color, true);
			writeBlockModel(color.normalName);
			writeBlockModel(color.voidName);
			writeItemModel(color.normalName);
			writeItemModel(color.voidName);
			writeBlockState(color.normalName);
			writeBlockState(color.voidName);
			writeLootTable(color.normalName);
			writeLootTable(color.voidName);
		}
	}

	public static void generateImage(CloudColor cloudColor, boolean isVoid) throws IOException {
		BufferedImage image = new BufferedImage(resolution, frames * resolution, BufferedImage.TYPE_INT_ARGB);
		int[] pixel = new int[1];
		for (int frame = 0; frame < frames; frame++) {
			int startY = frame * resolution;
			double time = ((double)(frame)) / ((double)(frames));
			for (int offsetY = 0; offsetY < resolution; offsetY++) {
				for (int offsetX = 0; offsetX < resolution; offsetX++) {
					pixel[0] = CloudColor.packARGB(getColor((offsetX + 0.5D) * (1.0D / resolution), (offsetY + 0.5D) * (1.0D / resolution), time, isVoid, cloudColor.getColor(time)));
					image.getRaster().setDataElements(offsetX, startY + offsetY, pixel);
				}
			}
		}
		writeImage(image, isVoid ? cloudColor.voidName : cloudColor.normalName);
	}

	public static void writeImage(BufferedImage image, String fileName) throws IOException {
		File file = new File(BASE_PATH, "textures" + File.separatorChar + fileName + ".png");
		file.getParentFile().mkdirs();
		ImageIO.write(image, "png", file);
		try (FileWriter writer = new FileWriter(file.getPath() + ".mcmeta", StandardCharsets.UTF_8)) {
			writer.write("{ \"animation\": {} }");
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
					"type": "minecraft:block",
					"pools": [{
						"rolls": 1,
						"entries": [{
							"type": "minecraft:item",
							"name": "bigglobe:%NAME"
						}],
						"conditions": [{
							"condition": "minecraft:survives_explosion"
						}]
					}]
				}""".replace("%NAME", fileName)
			);
		}
	}

	public static Vector3dc getColor(double uvX, double uvY, double time, boolean isVoid, Vector3dc glowColor) {
		double worley = worley(uvX * wrap, uvY * wrap, time);

		double background = Permuter.toPositiveDouble(Permuter.permute(SEED, floorI(uvX * resolution), floorI(uvY * resolution))) * 0.25D + 0.25D / (worley + 0.25D);
		background = isVoid ? background * 0.125D : background * 0.5D + 0.375D;

		if (glowColor != null) {
			double glow = worley;
			glow *= uvX - uvX * uvX;
			glow *= uvY - uvY * uvY;
			glow *= 16.0D;

			return new Vector3d(
				background + glowColor.x() * glow,
				background + glowColor.y() * glow,
				background + glowColor.z() * glow
			);
		}
		else {
			return new Vector3d(background);
		}
	}

	public static double worley(double coordX, double coordY, double time) {
		int startX = floorI(coordX);
		int startY = floorI(coordY);
		double nearestDistance = Double.POSITIVE_INFINITY;
		for (int offsetY = -2; offsetY <= 2; offsetY++) {
			for (int offsetX = -2; offsetX <= 2; offsetX++) {
				int cellX = startX + offsetX;
				int cellY = startY + offsetY;
				int wrappedX = modulus_BP(cellX, wrap);
				int wrappedY = modulus_BP(cellY, wrap);
				double centerX = cellX + (sin((time + Permuter.toPositiveDouble(Permuter.permute(SEED, 0, wrappedX, wrappedY))) * TAU) * 0.5D + 0.5D);
				double centerY = cellY + (sin((time + Permuter.toPositiveDouble(Permuter.permute(SEED, 1, wrappedX, wrappedY))) * TAU) * 0.5D + 0.5D);
				nearestDistance = min(nearestDistance, squareD(coordX - centerX, coordY - centerY));
			}
		}
		return nearestDistance;
	}
}