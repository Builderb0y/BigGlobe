package builderb0y.bigglobe.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.Mirror;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.versions.AutoCodecVersions;

@Mirror(UseVerifier.class)
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@UseVerifier(name = "verify", in = VerifyDivisibleBy16.Verifier.class, usage = MemberUsage.METHOD_IS_HANDLER)
public @interface VerifyDivisibleBy16 {

	public static class Verifier {

		public static <T_Encoded> void verify(VerifyContext<T_Encoded, Integer> context) throws VerifyException {
			Integer value = context.object;
			if (value != null && (value.intValue() & 15) != 0) {
				throw AutoCodecVersions.newVerifyException(() -> context.pathToStringBuilder().append(" must be divisible by 16.").toString());
			}
		}
	}
}