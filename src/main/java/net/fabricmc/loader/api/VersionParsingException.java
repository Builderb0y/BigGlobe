package net.fabricmc.loader.api;

public class VersionParsingException extends Exception {

	public VersionParsingException(String message) {
		super(message);
	}

	public VersionParsingException(String message, Throwable cause) {
		super(message, cause);
	}
}
