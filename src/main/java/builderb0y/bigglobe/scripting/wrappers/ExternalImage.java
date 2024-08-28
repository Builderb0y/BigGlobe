package builderb0y.bigglobe.scripting.wrappers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;

import javax.imageio.ImageIO;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.InfoHolder;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ExternalImage {

	public static final AbstractConstantFactory CONSTANT_FACTORY = new AbstractConstantFactory(TypeInfos.STRING, TypeInfo.of(ExternalImage.class)) {

		public final MethodInfo constantMethod = MethodInfo.getMethod(ExternalImage.class, "of");

		@Override
		public InsnTree createConstant(ConstantValue constant) {
			return ldc(this.constantMethod, constant);
		}

		@Override
		public InsnTree createNonConstant(InsnTree tree) {
			throw new UnsupportedOperationException("ExternalImage path must be a constant value");
		}
	};

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo width, height;
		public MethodInfo get;
	}

	public static final MutableScriptEnvironment ENVIRONMENT = (
		new MutableScriptEnvironment()
		.addType("ExternalImage", INFO.type)
		.addCastConstant(CONSTANT_FACTORY, true)
		.addFieldGet(INFO.width)
		.addFieldGet(INFO.height)
		.addMethodInvoke("", INFO.get)
	);

	public static class WeakIntArray extends WeakReference<int[]> {

		public static final ReferenceQueue<int[]> QUEUE = new ReferenceQueue<>();

		public final int hashCode;

		public WeakIntArray(int[] array) {
			super(array, QUEUE);
			this.hashCode = Arrays.hashCode(array);
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof WeakIntArray that && Arrays.equals(this.get(), that.get());
		}
	}

	public static final ObjectOpenHashSet<WeakIntArray> INTERNER = new ObjectOpenHashSet<>(16) {

		@Override
		public synchronized WeakIntArray addOrGet(WeakIntArray array) {
			WeakIntArray result = super.addOrGet(array);
			for (WeakIntArray toRemove; (toRemove = (WeakIntArray)(WeakIntArray.QUEUE.poll())) != null;) {
				this.remove(toRemove);
			}
			return result;
		}
	};

	public final int width, height;
	public final int[] data;

	public ExternalImage(int width, int height, int[] data) {
		this.width = width;
		this.height = height;
		this.data = Objects.requireNonNullElse(INTERNER.addOrGet(new WeakIntArray(data)).get(), data);
	}

	public static ExternalImage of(MethodHandles.Lookup caller, String name, Class<?> type, String id) throws IOException {
		Identifier identifier = Identifier.of(id);
		identifier = Identifier.of(identifier.getNamespace(), "bigglobe_external/" + identifier.getPath() + ".png");
		try (InputStream stream = BigGlobeMod.getResourceFactory().open(identifier)) {
			BufferedImage image = ImageIO.read(stream);
			int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			return new ExternalImage(image.getWidth(), image.getHeight(), pixels);
		}
	}

	public int get(int x, int y) {
		return this.data[Objects.checkIndex(y, this.height) * this.width + Objects.checkIndex(x, this.width)];
	}

	public static class ColorScriptEnvironment {

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
			red   = Interpolator.clamp(0, 255, red);
			green = Interpolator.clamp(0, 255, green);
			blue  = Interpolator.clamp(0, 255, blue);
			return 0xFF000000 | (red << 16) | (green << 8) | blue;
		}

		public static int packF(float red, float green, float blue) {
			return packI((int)(red * 255.0F + 0.5F), (int)(green * 255.0F + 0.5F), (int)(blue * 255.0F + 0.5F));
		}

		public static int packD(double red, double green, double blue) {
			return packI((int)(red * 255.0D + 0.5D), (int)(green * 255.0D + 0.5D), (int)(blue * 255.0D + 0.5D));
		}

		public static int packAI(int red, int green, int blue, int alpha) {
			red   = Interpolator.clamp(0, 255, red);
			green = Interpolator.clamp(0, 255, green);
			blue  = Interpolator.clamp(0, 255, blue);
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
}