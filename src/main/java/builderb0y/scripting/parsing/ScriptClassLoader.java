package builderb0y.scripting.parsing;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;

/**
the ClassLoader responsible for converting script bytecode into actual classes.
the scripts themselves are loaded from data packs. many built-in scripts
can be found in data/bigglobe/worldgen/configured_feature/<dimension>/.

I can't use {@link MethodHandles.Lookup#defineHiddenClass(byte[], boolean, ClassOption...)}
because scripts themselves can define their own classes too, and reference them,
but if the script and the user-defined class are both hidden, then neither can reference the other.

I also don't want to use {@link MethodHandles.Lookup#defineClass(byte[])},
because it ensures a strong reachability link between
the defined class and the class loader of the caller,
and I want to ensure my defined classes are unloadable whenever they
are no longer needed, which is the case when the world unloads.

so, my solution is to instantiate a new ScriptClassLoader for each script.
the ScriptClassLoader can only {@link #findClass(String) find} classes
which were defined with the script associated with the class loader.
*/
public class ScriptClassLoader extends ClassLoader {

	public static final MethodInfo GET_CONSTANT = MethodInfo.getMethod(ScriptClassLoader.class, "getConstant");

	public static final AtomicInteger CLASS_UNIQUIFIER = new AtomicInteger();

	public ClassCompileContext clazz;
	public Map<String, ClassCompileContext> loadable;

	public ScriptClassLoader(ClassCompileContext clazz) {
		super(ScriptClassLoader.class.getClassLoader());
		this.clazz = clazz;
		this.loadable = new HashMap<>(2);
		this.recursiveAddClasses(clazz);
	}

	public Class<?> defineMainClass() throws ClassNotFoundException {
		return this.loadClass(this.clazz.info.getClassName());
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

	public static Object getConstant(MethodHandles.Lookup lookup, Class<?> type, String name, int which) {
		if (lookup.lookupClass().getClassLoader() instanceof ScriptClassLoader loader) {
			ClassCompileContext context = loader.loadable.get(lookup.lookupClass().getName());
			if (context != null) {
				if (which >= 0 && which < context.constants.size()) {
					return context.constants.get(which);
				}
				else {
					throw new IndexOutOfBoundsException("Invalid constant with index " + which);
				}
			}
			else {
				throw new IllegalStateException("No context found for " + lookup.lookupClass());
			}
		}
		else {
			throw new IllegalCallerException("getConstant() can only be called by script classes");
		}
	}
}