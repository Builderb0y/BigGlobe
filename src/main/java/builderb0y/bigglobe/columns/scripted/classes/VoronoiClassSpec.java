package builderb0y.bigglobe.columns.scripted.classes;

import java.lang.reflect.Field;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

@UseVerifier(name = "verify", in = VoronoiClassSpec.class, usage = MemberUsage.METHOD_IS_HANDLER)
public class VoronoiClassSpec extends BaseClassSpec {

	public final @VerifyFloatRange(min = 0.0D) double weight;

	public VoronoiClassSpec(
		@IdentifierName String name,
		boolean isAbstract,
		@Nullable RegistryEntry<ElementSpec> parent,
		double weight,
		DelayedEntryList<ElementSpec> members
	) {
		super(name, isAbstract, parent, members);
		this.weight = weight;
	}

	public static <T_Encoded> void verify(VerifyContext<T_Encoded, VoronoiClassSpec> context) throws VerifyException {
		VoronoiClassSpec spec = context.object;
		if (spec == null) return;

		if (spec.isAbstract && spec.weight > 0.0D) {
			throw new VerifyException(() -> "Abstract voronoi classes cannot have a weight.");
		}
	}

	@Override
	public FieldInfo baseColumnField() {
		return VoronoiBase.INFO.column;
	}

	@Override
	@MustBeInvokedByOverriders
	public void addReservedMembers() {
		super.addReservedMembers();
		this.overrideTracker.addReservedField("column");
		this.overrideTracker.addReservedField("center_column");
		for (Field field : VoronoiBase.Info.class.getDeclaredFields()) {
			if (field.getType() == MethodInfo.class) try {
				MethodInfo info = (MethodInfo)(field.get(VoronoiBase.INFO));
				this.overrideTracker.addReservedMethod(info.name, info.paramTypes);
			}
			catch (IllegalAccessException exception) {
				throw new RuntimeException(exception);
			}
		}
	}
}