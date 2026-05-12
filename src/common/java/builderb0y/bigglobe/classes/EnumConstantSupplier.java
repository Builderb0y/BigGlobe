package builderb0y.bigglobe.classes;

import java.util.Map;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public interface EnumConstantSupplier {

	public static final Info INFO = new Info();

	public static class Info extends InfoHolder {

		public MethodInfo
			getZ,
			getB,
			getC,
			getS,
			getI,
			getL,
			getF,
			getD,
			getA;
	}

	public abstract boolean getZ(String name, boolean fallback);

	public abstract byte getB(String name, byte fallback);

	public abstract char getC(String name, char fallback);

	public abstract short getS(String name, short fallback);

	public abstract int getI(String name, int fallback);

	public abstract long getL(String name, long fallback);

	public abstract float getF(String name, float fallback);

	public abstract double getD(String name, double fallback);

	public abstract Object getA(String name, Object fallback);

	public static EnumConstantSupplier forMap(Map<String, Object> map) {
		return new EnumConstantSupplier() {

			@Override
			public boolean getZ(String name, boolean fallback) {
				Object value = map.get(name);
				return value != null ? ((Boolean)(value)).booleanValue() : fallback;
			}

			@Override
			public byte getB(String name, byte fallback) {
				Object value = map.get(name);
				return value != null ? ((Byte)(value)).byteValue() : fallback;
			}

			@Override
			public char getC(String name, char fallback) {
				Object value = map.get(name);
				return value != null ? ((Character)(value)).charValue() : fallback;
			}

			@Override
			public short getS(String name, short fallback) {
				Object value = map.get(name);
				return value != null ? ((Short)(value)).shortValue() : fallback;
			}

			@Override
			public int getI(String name, int fallback) {
				Object value = map.get(name);
				return value != null ? ((Integer)(value)).intValue() : fallback;
			}

			@Override
			public long getL(String name, long fallback) {
				Object value = map.get(name);
				return value != null ? ((Long)(value)).longValue() : fallback;
			}

			@Override
			public float getF(String name, float fallback) {
				Object value = map.get(name);
				return value != null ? ((Float)(value)).floatValue() : fallback;
			}

			@Override
			public double getD(String name, double fallback) {
				Object value = map.get(name);
				return value != null ? ((Double)(value)).doubleValue() : fallback;
			}

			@Override
			public Object getA(String name, Object fallback) {
				return map.getOrDefault(name, fallback);
			}
		};
	}
}