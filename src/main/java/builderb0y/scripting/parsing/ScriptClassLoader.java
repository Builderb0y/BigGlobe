package builderb0y.scripting.parsing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.file.PathUtils;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.scripting.ScriptLogger;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.optimization.ClassOptimizer;

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

	public final Map<String, ClassCompileContext> loadable;

	public ScriptClassLoader() {
		super(ScriptClassLoader.class.getClassLoader());
		this.loadable = new ConcurrentHashMap<>(8);
	}

	public static @Nullable Path initDumpDirectory(String enabledProperty, String directoryName) {
		if (Boolean.getBoolean(enabledProperty)) {
			Path classDumpDirectory = FabricLoader.getInstance().getGameDir().resolve(directoryName);
			if (Files.isDirectory(classDumpDirectory)) try {
				PathUtils.cleanDirectory(classDumpDirectory);
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error(
					"""
					An error occurred while trying to clean the previous session's script dump output.
					Dumping of generated classes has been disabled to prevent ambiguity over which file is from which session.
					Please empty the class dump directory manually when you get a chance.
					""",
					exception
				);
				return null;
			}
			else try {
				Files.createDirectory(classDumpDirectory);
			}
			catch (IOException exception) {
				ScriptLogger.LOGGER.error(
					"""
					An error occurred while trying to create the script dump directory.
					Dumping of generated classes has been disabled as there is nowhere to put them.
					""",
					exception
				);
				return null;
			}
			return classDumpDirectory;
		}
		else {
			return null;
		}
	}

	public Class<?> defineClass(ClassCompileContext clazz) throws ClassNotFoundException {
		this.recursiveAddClasses(clazz);
		return this.loadClass(clazz.info.getClassName());
	}

	public void recursiveAddClasses(ClassCompileContext clazz) {
		ClassOptimizer.DEFAULT.optimize(clazz.node);
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

	public static Object getConstant(MethodHandles.Lookup lookup, String name, Class<?> type, int which) {
		if (lookup.lookupClass().getClassLoader() instanceof ScriptClassLoader loader) {
			ClassCompileContext context = loader.loadable.get(lookup.lookupClass().getName());
			if (context != null) {
				if (which >= 0 && which < context.constants.size()) {
					return type.cast(context.constants.get(which));
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