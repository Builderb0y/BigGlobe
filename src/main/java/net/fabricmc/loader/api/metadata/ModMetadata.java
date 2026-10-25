package net.fabricmc.loader.api.metadata;

import net.fabricmc.loader.api.Version;

public interface ModMetadata {

	String getId();

	Version getVersion();

	String getName();
}
