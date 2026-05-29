package builderb0y.bigglobe.classes;

import builderb0y.scripting.bytecode.TypeInfo;

public class ScriptObject implements Cloneable {

	public static final TypeInfo TYPE = TypeInfo.of(ScriptObject.class);

	@Override
	public ScriptObject clone() {
		try {
			return (ScriptObject)(super.clone());
		}
		catch (CloneNotSupportedException exception) {
			throw new AssertionError(exception);
		}
	}
}