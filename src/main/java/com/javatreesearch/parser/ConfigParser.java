package com.javatreesearch.parser;

import com.javatreesearch.model.ConfigEntry;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads and caches configuration files (.properties, .yaml, .yml).
 */
public interface ConfigParser {
    /** Returns entries from the config file that match the given term, using cache. */
    List<ConfigEntry> getEntries(Path file, String term);

    /** Lists all config files (.properties, .yaml, .yml) under rootDir. */
    List<Path> listConfigFiles(Path rootDir);
}
