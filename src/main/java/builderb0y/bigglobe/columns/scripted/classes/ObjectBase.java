package builderb0y.bigglobe.columns.scripted.classes;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.util.InfoHolder;

public class ObjectBase {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo column;
	}

	public final ScriptedColumn column;

	public ObjectBase(ScriptedColumn column) {
		this.column = column;
	}
}