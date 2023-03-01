package builderb0y.scripting.bytecode;

public enum ClassType {
	ANNOTATION(true),
	ARRAY(false),
	CLASS(false),
	ENUM(false),
	INTERFACE(true),
	PRIMITIVE(false),
	RECORD(false);

	public final boolean isInterface;

	ClassType(boolean isInterface) {
		this.isInterface = isInterface;
	}
}