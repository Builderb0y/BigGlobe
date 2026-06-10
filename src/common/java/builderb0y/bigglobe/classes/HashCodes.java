package builderb0y.bigglobe.classes;

import it.unimi.dsi.fastutil.HashCommon;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class HashCodes {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public MethodInfo
			hashZ,
			hashB,
			hashC,
			hashS,
			hashI,
			hashL,
			hashF,
			hashD,
			hashA;
	}

	public static int hashZ(boolean value) { return Boolean.hashCode(value); }
	public static int hashB(byte    value) { return HashCommon.mix(value); }
	public static int hashC(char    value) { return HashCommon.mix(value); }
	public static int hashS(short   value) { return HashCommon.mix(value); }
	public static int hashI(int     value) { return HashCommon.mix(value); }
	public static int hashL(long    value) { return (int)(HashCommon.mix(value)); }
	public static int hashF(float   value) { return HashCommon.mix(Float.floatToIntBits(value)); }
	public static int hashD(double  value) { return (int)(HashCommon.mix(Double.doubleToLongBits(value))); }
	public static int hashA(Object  value) { return value == null ? 0 : HashCommon.mix(value.hashCode()); }
}