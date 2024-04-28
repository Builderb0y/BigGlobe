package builderb0y.bigglobe.datagen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.joml.Vector3dc;

import builderb0y.bigglobe.blocks.CloudColor;
import builderb0y.bigglobe.math.Interpolator;

import static builderb0y.bigglobe.datagen.CloudDataGenerator.frames;
import static builderb0y.bigglobe.datagen.CloudDataGenerator.resolution;

public class BottledAuraDataGenerator {

	public static final File
		WIP = new File("src/main/resources/wip"),
		BASE_PATH = new File(WIP, "bottled_aura");

	public static void main(String[] args) throws IOException {
		BASE_PATH.mkdirs();
		File textures = new File(BASE_PATH, "textures");
		File models = new File(BASE_PATH, "models");
		textures.mkdirs();
		models.mkdirs();
		BufferedImage bottle = ImageIO.read(new File(WIP, "aura_bottle.png"));
		BufferedImage aura = ImageIO.read(new File(WIP, "aura.png"));
		byte[] readPixel = new byte[4];
		int[] writePixel = new int[1];
		for (CloudColor color : CloudColor.VALUES) {
			BufferedImage texture;
			if (color == CloudColor.BLANK) {
				texture = null;
			}
			else if (color == CloudColor.RAINBOW) {
				texture = new BufferedImage(resolution, resolution * frames, BufferedImage.TYPE_INT_ARGB);
				for (int frame = 0; frame < frames; frame++) {
					for (int y = 0; y < resolution; y++) {
						for (int x = 0; x < resolution; x++) {
							aura.getRaster().getDataElements(x, y, readPixel);
							if (readPixel[0] != 0) {
								writePixel[0] = multiplyColor(readPixel, color.getColor(((double)(frame)) / ((double)(frames))));
							}
							else {
								bottle.getRaster().getDataElements(x, y, readPixel);
								transferColor(readPixel, writePixel);
							}
							texture.getRaster().setDataElements(x, y + frame * resolution, writePixel);
						}
					}
				}
				ImageIO.write(texture, "png", new File(textures, "omni_bottled_aura.png"));
			}
			else {
				texture = new BufferedImage(resolution, resolution, BufferedImage.TYPE_INT_ARGB);
				for (int y = 0; y < resolution; y++) {
					for (int x = 0; x < resolution; x++) {
						aura.getRaster().getDataElements(x, y, readPixel);
						if (readPixel[0] != 0) {
							writePixel[0] = multiplyColor(readPixel, color.color);
						}
						else {
							bottle.getRaster().getDataElements(x, y, readPixel);
							transferColor(readPixel, writePixel);
						}
						texture.getRaster().setDataElements(x, y, writePixel);
					}
				}
				ImageIO.write(texture, "png", new File(textures, color.bottleName + ".png"));
			}
			if (texture != null) {
				try (FileWriter writer = new FileWriter(new File(models, color.bottleName + ".json"), StandardCharsets.UTF_8)) {
					writer.write(
						"""
						{
							"parent": "item/generated",
							"textures": {
								"layer0": "bigglobe:item/%NAME"
							}
						}""".replace("%NAME", color.bottleName)
					);
				}
			}
		}
	}

	public static int multiplyColor(byte[] color, Vector3dc tint) {
		double red   = (color[0] & 255) * tint.x() + 0.5D;
		double green = (color[1] & 255) * tint.y() + 0.5D;
		double blue  = (color[2] & 255) * tint.z() + 0.5D;
		int r = Interpolator.clamp(0, 255, (int)(red  ));
		int g = Interpolator.clamp(0, 255, (int)(green));
		int b = Interpolator.clamp(0, 255, (int)(blue ));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public static void transferColor(byte[] from, int[] to) {
		to[0] = ((from[3] & 255) << 24) | ((from[2] & 255)) | ((from[1] & 255) << 8) | ((from[0] & 255) << 16);
	}
}