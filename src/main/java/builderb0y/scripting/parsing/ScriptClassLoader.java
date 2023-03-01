package builderb0y.scripting.parsing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import builderb0y.scripting.bytecode.ClassCompileContext;

public class ScriptClassLoader extends ClassLoader {

	public static final AtomicInteger CLASS_UNIQUIFIER = new AtomicInteger();

	public ClassCompileContext clazz;
	public Map<String, ClassCompileContext> loadable;

	public ScriptClassLoader(ClassCompileContext clazz) {
		super(ScriptClassLoader.class.getClassLoader());
		this.clazz = clazz;
		this.loadable = new HashMap<>(2);
		this.recursiveAddClasses(clazz);
	}

	public Class<?> defineMainClass() {
		try {
			return this.loadClass(this.clazz.info.getClassName());
		}
		catch (ClassNotFoundException exception) {
			throw new AssertionError("Could not find main script class", exception);
		}
	}

	public void recursiveAddClasses(ClassCompileContext clazz) {
		this.loadable.put(clazz.info.getClassName(), clazz);
		for (ClassCompileContext innerClass : clazz.innerClasses) {
			this.recursiveAddClasses(innerClass);
		}
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		ClassCompileContext clazz = this.loadable.get(name.replace('/', '.'));
		if (clazz != null) {
			byte[] bytes = clazz.toByteArray();
			return this.defineClass(clazz.info.getClassName(), bytes, 0, bytes.length);
		}
		else {
			throw new ClassNotFoundException(name);
		}
	}
}