# Java Tree Search

A CLI tool for recursive AST-based code search in Java repositories. Given a search term, it locates where the term appears in `.java`, `.properties`, `.yaml`, and `.yml` files, extracts the reference found, and recursively tracks where that reference is used throughout the codebase — up to a configurable depth.

## How it works

1. **Iteration 1** — finds the search term in source and config files
2. **Iteration 2+** — for each reference found, tracks its usages across the repository
3. Repeats until no more references exist or the depth limit is reached

Supports Spring Boot (`@Value`) and Quarkus (`@ConfigProperty`) annotation-based config key injection.

## Build

Requires Java 17+ and Maven 3.8+.

```bash
mvn package -f java-tree-search/pom.xml -DskipTests
```

The fat JAR will be generated at `java-tree-search/target/java-tree-search.jar`.

## Usage

```bash
java -jar java-tree-search/target/java-tree-search.jar <searchTerm> [directory] [depth]
```

| Argument      | Required | Default           | Description                        |
|---------------|----------|-------------------|------------------------------------|
| `searchTerm`  | yes      | —                 | Term to search for                 |
| `directory`   | no       | current directory | Root directory of the repository   |
| `depth`       | no       | `5`               | Maximum recursion depth (positive integer) |

### Examples

```bash
# Search for "myProperty" in the current directory with default depth
java -jar java-tree-search/target/java-tree-search.jar myProperty

# Search in a specific repo with depth 3
java -jar java-tree-search/target/java-tree-search.jar myProperty /path/to/repo 3
```

### Output

When running in a terminal, results are displayed in a hierarchical format with `DEF` (definition) and `USE` (usage) markers. When stdout is redirected to a file or pipe, output is produced as structured JSON.

## Run tests

```bash
mvn test -f java-tree-search/pom.xml
```

## License

Copyright 2024 Java Tree Search Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
