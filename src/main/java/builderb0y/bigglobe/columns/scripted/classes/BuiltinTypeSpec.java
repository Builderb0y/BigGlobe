package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.StringIdentifiable;

import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.util.TypeInfos;

public class BuiltinTypeSpec extends TypeSpec {

	public final BuiltinJavaType java_type;

	public BuiltinTypeSpec(BuiltinJavaType java_type) {
		this.java_type = java_type;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return switch (this.java_type) {
			case BYTE    -> TypeInfos.BYTE;
			case SHORT   -> TypeInfos.SHORT;
			case INT     -> TypeInfos.INT;
			case LONG    -> TypeInfos.LONG;
			case FLOAT   -> TypeInfos.FLOAT;
			case DOUBLE  -> TypeInfos.DOUBLE;
			case CHAR    -> TypeInfos.CHAR;
			case BOOLEAN -> TypeInfos.BOOLEAN;
			case VOID    -> TypeInfos.VOID;
		};
	}

	@Override
	public boolean isFinal() {
		return true;
	}

	@Override
	public @Nullable OverrideTracker getOverrideTracker() {
		return null;
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, ClassCompileContext caller) {
		//no-op. these types are already provided by BuiltinScriptEnvironment.
	}

	@Override
	public String name() {
		return this.java_type.lowerCaseName;
	}

	public static enum BuiltinJavaType implements StringIdentifiable {
		BYTE,
		SHORT,
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		CHAR,
		BOOLEAN,
		VOID;

		public final String lowerCaseName = this.name().toLowerCase(Locale.ROOT);

		@Override
		public String asString() {
			return this.lowerCaseName;
		}
	}
}