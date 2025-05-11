package builderb0y.bigglobe.scripting.environments;

import builderb0y.bigglobe.math.Interpolator;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;

public class ColorScriptEnvironment {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			redI, greenI, blueI, alphaI,
			redF, greenF, blueF, alphaF,
			redD, greenD, blueD, alphaD,
			packI, packF, packD,
			packAI, packAF, packAD;
	}

	public static final MutableScriptEnvironment ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addFieldInvokeStatic(INFO.redI)
		.addFieldInvokeStatic(INFO.greenI)
		.addFieldInvokeStatic(INFO.blueI)
		.addFieldInvokeStatic(INFO.alphaI)
		.addFieldInvokeStatic(INFO.redF)
		.addFieldInvokeStatic(INFO.greenF)
		.addFieldInvokeStatic(INFO.blueF)
		.addFieldInvokeStatic(INFO.alphaF)
		.addFieldInvokeStatic(INFO.redD)
		.addFieldInvokeStatic(INFO.greenD)
		.addFieldInvokeStatic(INFO.blueD)
		.addFieldInvokeStatic(INFO.alphaD)
		.addFunctionInvokeStatic(INFO.packI)
		.addFunctionInvokeStatic(INFO.packF)
		.addFunctionInvokeStatic(INFO.packD)
		.addFunctionInvokeStatic("packI", INFO.packAI)
		.addFunctionInvokeStatic("packF", INFO.packAF)
		.addFunctionInvokeStatic("packD", INFO.packAD)
	);

	public static int alphaI(int packed) {
		return (packed >>> 24);
	}

	public static int redI(int packed) {
		return (packed >>> 16) & 255;
	}

	public static int greenI(int packed) {
		return (packed >>> 8) & 255;
	}

	public static int blueI(int packed) {
		return packed & 255;
	}

	public static float alphaF(int packed) {
		return alphaI(packed) / 255.0F;
	}

	public static float redF(int packed) {
		return redI(packed) / 255.0F;
	}

	public static float greenF(int packed) {
		return greenI(packed) / 255.0F;
	}

	public static float blueF(int packed) {
		return blueI(packed) / 255.0F;
	}

	public static double alphaD(int packed) {
		return alphaI(packed) / 255.0D;
	}

	public static double redD(int packed) {
		return redI(packed) / 255.0D;
	}

	public static double greenD(int packed) {
		return greenI(packed) / 255.0D;
	}

	public static double blueD(int packed) {
		return blueI(packed) / 255.0D;
	}

	public static int packI(int red, int green, int blue) {
		red = Interpolator.clamp(0, 255, red);
		green = Interpolator.clamp(0, 255, green);
		blue = Interpolator.clamp(0, 255, blue);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	public static int packF(float red, float green, float blue) {
		return packI((int)(red * 255.0F + 0.5F), (int)(green * 255.0F + 0.5F), (int)(blue * 255.0F + 0.5F));
	}

	public static int packD(double red, double green, double blue) {
		return packI((int)(red * 255.0D + 0.5D), (int)(green * 255.0D + 0.5D), (int)(blue * 255.0D + 0.5D));
	}

	public static int packAI(int red, int green, int blue, int alpha) {
		red = Interpolator.clamp(0, 255, red);
		green = Interpolator.clamp(0, 255, green);
		blue = Interpolator.clamp(0, 255, blue);
		alpha = Interpolator.clamp(0, 255, alpha);
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	public static int packAF(float red, float green, float blue, float alpha) {
		return packAI((int)(red * 255.0F + 0.5F), (int)(green * 255.0F + 0.5F), (int)(blue * 255.0F + 0.5F), (int)(alpha * 255.0F + 0.5F));
	}

	public static int packAD(double red, double green, double blue, double alpha) {
		return packAI((int)(red * 255.0D + 0.5D), (int)(green * 255.0D + 0.5D), (int)(blue * 255.0D + 0.5D), (int)(alpha * 255.0D + 0.5D));
	}
}