package builderb0y.bigglobe.columns.scripted.classes;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ConstructorInfo;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.util.InfoHolder;

public class ObjectBase {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo column;
		@Disambiguate(name = "<init>", returnType = void.class, paramTypes = { ScriptedColumn.class })
		public MethodInfo constructor;
	}
	public static final ConstructorInfo CONSTRUCTOR_INFO = new ConstructorInfo(ObjectBase.class);

	public final ScriptedColumn column;

	public ObjectBase(ScriptedColumn column) {
		this.column = column;
	}
}