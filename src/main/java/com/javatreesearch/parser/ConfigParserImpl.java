package com.javatreesearch.parser;

import com.javatreesearch.model.ConfigEntry;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of ConfigParser with lazy caching.
 * Supports .properties, .yaml, and .yml files.
 *
 * Cache strategy:
 * 1. Check cache for existing entries.
 * 2. If absent: read file, parse all entries, store in cache.
 * 3. Filter cached entries by term on each call.
 *
 * Error handling:
 * - I/O or parse error with other files present: throw ConfigIoException (caller logs warning, continues).
 * - I/O or parse error on only file: caller aborts.
 */
public class ConfigParserImpl implements ConfigParser {

    private final Map<Path, List<ConfigEntry>> cache = new HashMap<>();

    @Override
    public List<ConfigEntry> getEntries(Path file, String term) {
        if (!cache.containsKey(file)) {
            List<ConfigEntry> entries = parseFile(file);
            cache.put(file, entries);
        }
        return cache.get(file).stream()
            .filter(e -> e.key().contains(term) || e.value().contains(term))
            .toList();
    }

    @Override
    public List<Path> listConfigFiles(Path rootDir) {
        try (Stream<Path> stream = Files.walk(rootDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.toString();
                    return name.endsWith(".properties") || name.endsWith(".yaml") || name.endsWith(".yml");
                })
                .toList();
        } catch (IOException e) {
            System.err.println("[WARN] Failed to traverse directory " + rootDir + ": " + e.getMessage());
            return List.of();
        }
    }

    private List<ConfigEntry> parseFile(Path file) {
        String name = file.toString();
        try {
            if (name.endsWith(".properties")) {
                return parseProperties(file);
            } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                return parseYaml(file);
            }
            return List.of();
        } catch (IOException e) {
            throw new ConfigIoException(file, e);
        }
    }

    private List<ConfigEntry> parseProperties(Path file) throws IOException {
        List<ConfigEntry> entries = new ArrayList<>();
        String content = Files.readString(file);
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                // Skip comments and blank lines
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                int eqIdx = trimmed.indexOf('=');
                int colonIdx = trimmed.indexOf(':');
                int sepIdx = -1;
                if (eqIdx >= 0 && colonIdx >= 0) {
                    sepIdx = Math.min(eqIdx, colonIdx);
                } else if (eqIdx >= 0) {
                    sepIdx = eqIdx;
                } else if (colonIdx >= 0) {
                    sepIdx = colonIdx;
                }
                if (sepIdx > 0) {
                    String key = trimmed.substring(0, sepIdx).trim();
                    String value = trimmed.substring(sepIdx + 1).trim();
                    entries.add(new ConfigEntry(key, value, file, lineNumber));
                }
            }
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<ConfigEntry> parseYaml(Path file) throws IOException {
        String content = Files.readString(file);
        LoadSettings settings = LoadSettings.builder().build();
        Load load = new Load(settings);
        List<ConfigEntry> entries = new ArrayList<>();
        for (Object parsed : load.loadAllFromString(content)) {
            if (parsed instanceof Map) {
                flattenYaml("", (Map<Object, Object>) parsed, content, file, entries);
            }
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private void flattenYaml(String prefix, Map<Object, Object> map, String content,
                              Path file, List<ConfigEntry> entries) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty()
                ? String.valueOf(entry.getKey())
                : prefix + "." + entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Map) {
                flattenYaml(key, (Map<Object, Object>) val, content, file, entries);
            } else {
                String value = val == null ? "" : String.valueOf(val);
                // Approximate line number by searching for the key in the content
                int lineNum = findLineNumber(content, String.valueOf(entry.getKey()));
                entries.add(new ConfigEntry(key, value, file, Math.max(1, lineNum)));
            }
        }
    }

    private int findLineNumber(String content, String key) {
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(key + ":") || lines[i].contains(key + " :")) {
                return i + 1;
            }
        }
        return 1;
    }

    /** Thrown when a config file cannot be read or parsed. */
    public static class ConfigIoException extends RuntimeException {
        private final Path file;

        public ConfigIoException(Path file, Exception cause) {
            super("Error reading config file " + file + ": " + cause.getMessage(), cause);
            this.file = file;
        }

        public Path getFile() {
            return file;
        }
    }
}
