package builderb0y.bigglobe.blocks;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;

public enum CloudColor {
	RED         (         "red_aura_infused_cloud",          "red_aura_infused_void_cloud",          "red_bottled_aura", smoothHue( 0.0D / 12.0D)),
	ORANGE      (      "orange_aura_infused_cloud",       "orange_aura_infused_void_cloud",       "orange_bottled_aura", smoothHue( 1.0D / 12.0D)),
	YELLOW      (      "yellow_aura_infused_cloud",       "yellow_aura_infused_void_cloud",       "yellow_bottled_aura", smoothHue( 2.0D / 12.0D)),
	YELLOW_GREEN("yellow_green_aura_infused_cloud", "yellow_green_aura_infused_void_cloud", "yellow_green_bottled_aura", smoothHue( 3.0D / 12.0D)),
	GREEN       (       "green_aura_infused_cloud",        "green_aura_infused_void_cloud",        "green_bottled_aura", smoothHue( 4.0D / 12.0D)),
	CYAN_GREEN  (  "cyan_green_aura_infused_cloud",   "cyan_green_aura_infused_void_cloud",   "cyan_green_bottled_aura", smoothHue( 5.0D / 12.0D)),
	CYAN        (        "cyan_aura_infused_cloud",         "cyan_aura_infused_void_cloud",         "cyan_bottled_aura", smoothHue( 6.0D / 12.0D)),
	CYAN_BLUE   (   "cyan_blue_aura_infused_cloud",    "cyan_blue_aura_infused_void_cloud",    "cyan_blue_bottled_aura", smoothHue( 7.0D / 12.0D)),
	BLUE        (        "blue_aura_infused_cloud",         "blue_aura_infused_void_cloud",         "blue_bottled_aura", smoothHue( 8.0D / 12.0D)),
	PURPLE      (      "purple_aura_infused_cloud",       "purple_aura_infused_void_cloud",       "purple_bottled_aura", smoothHue( 9.0D / 12.0D)),
	MAGENTA     (     "magenta_aura_infused_cloud",      "magenta_aura_infused_void_cloud",      "magenta_bottled_aura", smoothHue(10.0D / 12.0D)),
	MAGENTA_RED ( "magenta_red_aura_infused_cloud",  "magenta_red_aura_infused_void_cloud",  "magenta_red_bottled_aura", smoothHue(11.0D / 12.0D)),
	RAINBOW     (        "omni_aura_infused_cloud",         "omni_aura_infused_void_cloud",         "omni_bottled_aura", null) {

		@Override
		public Vector3dc getColor(double timeFraction) {
			return smoothHue(timeFraction);
		}
	},
	BLANK       (                          "cloud",                           "void_cloud", null, null);

	public static final CloudColor[] VALUES = values();

	public final String normalName, voidName, bottleName;
	public final @Nullable Vector3dc color;

	CloudColor(String normalName, String voidName, String bottleName, @Nullable Vector3dc color) {
		this.normalName = normalName;
		this.  voidName =   voidName;
		this.bottleName = bottleName;
		this.     color =      color;
	}

	public @Nullable Vector3dc getColor(double timeFraction) {
		return this.color;
	}

	public static Vector3d smoothHue(double hue) {
		hue *= BigGlobeMath.TAU;
		//coefficients.
		double red   = hue;
		double green = hue - BigGlobeMath.TAU        / 3.0D;
		double blue  = hue - BigGlobeMath.TAU * 2.0D / 3.0D;
		//base curve.
		red   = Math.cos(red  ) * 0.5D + 0.5D;
		green = Math.cos(green) * 0.5D + 0.5D;
		blue  = Math.cos(blue ) * 0.5D + 0.5D;
		//convert to linear space.
		red   *= red;
		green *= green;
		blue  *= blue;
		//normalize.
		double scalar = 1.0D / Math.sqrt(BigGlobeMath.squareD(red, green, blue));
		red   *= scalar;
		green *= scalar;
		blue  *= scalar;
		//convert back to gamma space.
		red   = Math.sqrt(red);
		green = Math.sqrt(green);
		blue  = Math.sqrt(blue);
		//done.
		return new Vector3d(red, green, blue);
	}

	public static int packARGB(Vector3dc color) {
		int r = Interpolator.clamp(0, 255, (int)(color.x() * 256.0D));
		int g = Interpolator.clamp(0, 255, (int)(color.y() * 256.0D));
		int b = Interpolator.clamp(0, 255, (int)(color.z() * 256.0D));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}