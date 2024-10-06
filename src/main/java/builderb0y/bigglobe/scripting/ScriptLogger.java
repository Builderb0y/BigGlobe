package builderb0y.bigglobe.scripting;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builderb0y.bigglobe.BigGlobeMod;

public class ScriptLogger {

	//use BuiltinScriptEnvironment.PRINTER instead.
	//unless you *only* want to print something to the console,
	//in which case suppress the deprecation warning.
	//@Deprecated
	public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/Scripting");

	public static String addLineNumbers(String source) {
		StringBuilder builder = new StringBuilder(source.length() + (source.length() >> 2));
		Iterator<String> iterator = source.lines().iterator();
		for (int line = 1; iterator.hasNext(); line++) {
			builder.append(line).append(":\t").append(iterator.next()).append('\n');
		}
		return builder.toString();
	}
}