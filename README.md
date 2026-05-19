# Nuke Build Tool

Nuke is a fast, lightweight, and extensible build tool for Java projects, configured entirely using [EDN (Extensible Data Notation)](https://github.com/edn-format/edn) and scripted in Coni.

## Features
- **EDN Configuration**: Define your project metadata, dependencies, and custom tasks in a simple `nuke.edn` file.
- **Dependency Management**: Automatically downloads dependencies from Maven Central or resolves them from local Nuke projects.
- **Built-in Tasks**: Standard build lifecycle out of the box (`clean`, `compile`, `test`, `run`, `jar`, `uberjar`, `zip`, `upload`, `build`).
- **Custom Tasks**: Easily define custom tasks in `nuke.edn` that can execute bash commands, run Coni scripts, or extend existing built-in tasks.
- **IDE Support**: Comes with an IntelliJ IDEA plugin for seamless integration, task execution, and classpath synchronization.
- **Native Templating**: Inject build variables into source files automatically via the `:templates` configuration.
- **No Boilerplate**: No XML, no verbose Gradle scripts—just a minimal EDN map.

## Installation

(Assuming the `nuke` binary wrapper is available in your `$PATH`)

## Usage

In your project root, run `nuke <task>`. If no task is provided, `nuke build` is executed by default.

### Common Commands

- `nuke compile` - Compile Java source files
- `nuke test` - Run JUnit tests
- `nuke run` - Run the Java application (requires `:main-class`)
- `nuke jar` - Create a standard thin jar
- `nuke uberjar` - Create an executable fat jar
- `nuke zip` - Create a distribution zip
- `nuke upload` - Upload the jar and POM to a Nexus repository
- `nuke tasks` - List all available tasks
- `nuke info` - Display project metadata

## Configuration (`nuke.edn`)

The build configuration is stored in `nuke.edn` in the root of your project.

### Example `nuke.edn`

```edn
{:name "my-awesome-app"
 :version "1.0.0"
 :repositories ["https://repo1.maven.org/maven2"]
 :dependencies ["org.apache.commons:commons-lang3:3.12.0"
                "junit:junit:4.13.2"]
 :main-class "com.example.Main"
 :javac-opts ["-parameters"]
 :encoding "UTF-8"
 :templates ["src/main/resources/config.txt.template"]
 :tasks {:custom-jar {:extends "jar"
                      :jar-name "out/my-app-custom.jar"
                      :desc "Creates a standard jar directly after compile, with a custom name"}
         :hello-world {:desc "Prints Hello World"
                       :cmds ["echo 'Hello World!'"]}
         :scripted {:desc "Runs a coni script"
                    :coni "(println \"Executing Coni logic...\")"}}}
```

### Configuration Keys

- `:name` - The project name (used for jar generation).
- `:version` - The project version.
- `:group-id` - The Maven group ID (used for Nexus upload/POM generation).
- `:repositories` - List of Maven repository URLs.
- `:dependencies` - List of Maven coordinates in the format `"group:artifact:version"`.
- `:local-dependencies` - List of local Nuke projects to build and link.
- `:templates` - List of template files to process (variables like `${name}` and `${version}` will be replaced, and the `.template` extension will be stripped from the output).
- `:main-class` - Fully qualified class name to execute with `nuke run` or to embed in Jar manifests.
- `:java-home` - Optional override for `$JAVA_HOME`.
- `:src-dir` - Source directory (default: `src/main`).
- `:test-dir` - Test source directory (default: `src/tests`).
- `:resource-dir` - Resource directory (default: `src/main/resources`).
- `:javac-opts` - List of arguments to pass to `javac`.
- `:encoding` - Source encoding (e.g., `UTF-8`).
- `:deploy` - Nexus deployment URL.
- `:tasks` - A map of custom task definitions.

## Custom Tasks

You can define custom tasks under the `:tasks` key in your `nuke.edn`.

- `:extends`: Inherits the behavior of an existing task (e.g., `"jar"` or `"uberjar"`) but allows you to override properties like `:jar-name`.
- `:cmds`: A list of shell commands to execute.
- `:coni`: A string containing Coni code to execute, or a path to a `.coni` file.
- `:deps`: A list of task dependencies that must run before this task.
- `:desc`: A short description shown in `nuke tasks`.

## Directory Structure

By default, Nuke expects a standard directory layout:

```text
.
├── nuke.edn
├── src/
│   ├── main/          # Java source files
│   ├── main/resources # Resources copied to jars
│   └── tests/         # JUnit test files (*Test.java)
├── libs/              # Downloaded dependencies
├── classes/           # Compiled main classes
├── test-classes/      # Compiled test classes
└── target/            # Generated jars and zips
```

## IDE Integration

Nuke provides a dedicated IntelliJ IDEA plugin. You can install it from the `nuke-intellij-plugin` directory.
- Features a **Nuke Build** tool window.
- Allows 1-click execution of any Nuke task.
- Adds "Sync Nuke Project" action to download dependencies and configure your module classpath automatically.
- Import dependencies automatically from existing `build.gradle` or `pom.xml` files directly from the tool window.
- Provides syntax highlighting and language support for `.edn` and `.coni` files.

## Under the Hood

Nuke is written entirely in Coni (`main.coni`) and leverages basic tools (`curl`, `javac`, `jar`, `java`, `zip`, `find`) to keep the build extremely fast and minimal without spinning up a heavy JVM daemon for the build logic itself.
