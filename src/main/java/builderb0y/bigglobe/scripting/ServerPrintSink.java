package builderb0y.bigglobe.scripting;

import builderb0y.scripting.util.PrintSink;

public class ServerPrintSink implements PrintSink {

	@Override public void println(int     value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(long    value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(float   value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(double  value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(char    value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(boolean value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
	@Override public void println(String  value) { ScriptLogger.LOGGER.info(               value ); }
	@Override public void println(Object  value) { ScriptLogger.LOGGER.info(String.valueOf(value)); }
}