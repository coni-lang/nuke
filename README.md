# Nuke Build Tool

Nuke is a fast, lightweight, and extensible build tool for Java projects, configured entirely using [EDN (Extensible Data Notation)](https://github.com/edn-format/edn) and scripted in Coni.

## Features
- **EDN Configuration**: Define your project metadata, dependencies, and custom tasks in a simple `nuke.edn` file.
- **Dependency Management**: Automatically downloads dependencies from Maven Central or resolves them from local Nuke projects.
- **Built-in Tasks**: Standard build lifecycle out of the box (`clean`, `compile`, `test`, `run`, `jar`, `uberjar`, `zip`, `upload`, `build`, `dependencies`).
- **Static Analysis & Metrics (New)**: First-class integration with JaCoCo (Coverage), SpotBugs, PMD, Checkstyle, Error Prone, and SonarQube. Automatically stitches results into a beautiful unified HTML dashboard!
- **Custom Tasks**: Easily define custom tasks in `nuke.edn` that can execute bash commands, run Coni scripts, or extend existing built-in tasks.
- **IDE Support**: Comes with an IntelliJ IDEA plugin for seamless integration, task execution, and classpath synchronization.
- **Native Templating**: Inject build variables into source files automatically via the `:templates` configuration.
- **No Boilerplate**: No XML, no verbose Gradle scripts—just a minimal EDN map.

## Installation

(Assuming the `nuke` binary wrapper is available in your `$PATH`)

## Usage

In your project root, run `nuke <task>`. If no task is provided, `nuke build` is executed by default.

### Common Commands

- `nuke init` - Scaffold a new Nuke project structure
- `nuke compile` - Compile Java source files (runs Error Prone if enabled, copies resources)
- `nuke test` - Run JUnit tests
- `nuke metrics` - Run tests with JaCoCo agent and generate coverage reports
- `nuke analyze` - Run full static analysis (SpotBugs, PMD, Checkstyle) and generate the unified `nuke-analysis.html` dashboard
- `nuke run` - Run the Java application (requires `:main-class`)
- `nuke jar` - Create a standard thin jar
- `nuke uberjar` - Create an executable fat jar
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
 :analysis {:jacoco {:version "0.8.12"}
            :error-prone {:enabled true}
            :sonarqube {:version "5.0.1.3006"
                        :host "https://sonar.example.com"
                        :token "sqp_xxx"}}
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
- `:test-dependencies` - List of Maven coordinates used exclusively during `nuke test` and excluded from `uberjar`.
- `:local-dependencies` - List of local Nuke projects to build and link.
- `:git-registries` - List of base git URLs used to resolve short dependency names (see [Git Dependencies](#git-dependencies)).
- `:git-dependencies` - List of git-based dependencies in `"name#ref"` or `"url#ref"` format (see [Git Dependencies](#git-dependencies)).
- `:analysis` - (New) Configuration block for JaCoCo, Error Prone, SonarQube, PMD, SpotBugs, and Checkstyle.
- `:templates` - List of template files to process (variables like `${name}` and `${version}` will be replaced, and the `.template` extension will be stripped from the output).
- `:main-class` - Fully qualified class name to execute with `nuke run` or to embed in Jar manifests.
- `:java-home` - Optional override for `$JAVA_HOME`.
- `:java-version` - Target Java release version (e.g., `"17"`), mapped to `javac --release`.
- `:java-source` / `:java-target` - Legacy options for `javac -source` and `-target` if `java-version` is unused.
- `:src-dir` - Source directory (default: `src/main`).
- `:test-dir` - Test source directory (default: `src/tests`).
- `:resource-dir` - Resource directory (default: `src/main/resources`). Automatically copied to `classes/`.
- `:javac-opts` - List of arguments to pass to `javac`.
- `:encoding` - Source encoding (e.g., `UTF-8`).
- `:deploy` - Nexus deployment URL (string) or a map of multiple deployment targets (e.g., `{:nexus1 "url1" :nexus2 "url2"}`).
- `:tasks` - A map of custom task definitions.

## Git Dependencies

Nuke supports pulling dependencies directly from git repositories, eliminating the need for a Nexus server for internal/team libraries. Dependencies are specified as `"name#ref"` where `ref` can be a **tag** (e.g., `v1.2.0`) or a **branch** (e.g., `main`, `develop`).

- **Tags** are immutable — once cloned and built, they are cached permanently under `~/.nuke/git-deps/`.
- **Branches** are re-fetched on each build. If new commits are detected, the dependency is automatically rebuilt.

### Basic Usage (full URLs)

```edn
{:name "my-app"
 :version "2.0.0"
 :git-dependencies ["https://gitea.klabs.home/nico/my-utils#v1.2.0"
                     "git@gitea.klabs.home:nico/other-lib#develop"]
 :main-class "com.example.Main"}
```

### Using Registries (short names)

Define `:git-registries` to avoid repeating base URLs. When a dependency has no `://` or `git@` prefix, Nuke tries each registry in order:

```edn
{:name "my-app"
 :version "2.0.0"
 :git-registries ["https://gitea.klabs.home/nico"
                   "git@gitea.klabs.home:team"]
 :git-dependencies ["my-utils#v1.2.0"
                     "shared-lib#main"
                     "https://github.com/external/lib#v0.5.0"]
 :main-class "com.example.Main"}
```

In this example, `my-utils#v1.2.0` will first try `https://gitea.klabs.home/nico/my-utils`, then `git@gitea.klabs.home:team/my-utils`. Full URLs like the GitHub one are used directly.

### Subfolder Dependencies (monorepo support)

Use `//` to reference a subdirectory within a repository. The repo is cloned once and the specified subfolder is built:

```edn
{:name "my-app"
 :version "2.0.0"
 :git-dependencies ["ssh://git@s5:2222/hellonico/nuke.git//example-math-lib#main"]
 :main-class "com.example.Main"}
```

This also works with registries:

```edn
{:name "my-app"
 :version "2.0.0"
 :git-registries ["ssh://git@s5:2222/hellonico"]
 :git-dependencies ["nuke//example-math-lib#main"
                     "nuke//example-java-lib#v2.0"]
 :main-class "com.example.Main"}
```

Multiple subfolders from the same repo share a single clone — only one git fetch is performed.

### Mixed Maven + Git Dependencies

Both `:dependencies` (Maven/Nexus) and `:git-dependencies` can coexist. All jars end up on the same classpath.
Additionally, Nuke features **Global Classpath Deduplication**. If your main project and git dependencies pull different versions of the same library (e.g., `guava-30` vs `guava-31`), Nuke resolves the conflict intelligently by keeping the nearest/highest declared version and preventing `NoSuchMethodError` classpath pollution.

```edn
{:name "my-app"
 :version "2.0.0"
 :repositories ["https://repo1.maven.org/maven2"]
 :dependencies ["com.google.guava:guava:32.1.2-jre"]
 :git-registries ["https://gitea.klabs.home/nico"]
 :git-dependencies ["my-utils#v1.2.0"]
 :main-class "com.example.Main"}
```

### Authentication

- **SSH** (`git@` or `ssh://`): Uses your standard SSH agent and key configuration. No extra setup needed.
- **HTTP(S)**: Set the `NUKE_GIT_USER` and `NUKE_GIT_PASSWORD` environment variables. Nuke will inject them into HTTP(S) clone URLs automatically.

### Transitive Git Dependencies

If a git dependency itself declares `:git-dependencies` in its `nuke.edn`, those are resolved recursively. Registries from both the parent and child projects are merged (child registries take precedence).

### Cache Management

Git dependencies are cached globally under `~/.nuke/git-deps/<host>/<owner>/<repo>/<ref>/`. To clear the cache:

```sh
nuke clean-git-deps
```

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

## Version History

### v1.2.0 (Latest)
- **Multiple Deploy Targets**: `:deploy` can now be a map of named repositories. Specify the target using `nuke upload <target-name>` or `nuke upload-uberjar <target-name>`. If target is omitted, Nuke will fail-fast and list available options. The IntelliJ plugin adds a gutter menu option for each deployment target.
- **Git-Based Dependencies**: Pull dependencies directly from git repositories instead of Nexus. Supports tags (cached permanently) and branches (re-fetched and rebuilt on new commits).
- **Git Registries**: Define `:git-registries` to avoid repeating base URLs for team/org repos.
- **Subfolder Dependencies**: Reference subdirectories within monorepos using `//` syntax (e.g., `"my-repo//libs/utils#v1.0"`). Multiple subfolders share a single clone.
- **SSH & HTTP Auth**: SSH repos use standard ssh-agent. HTTP(S) repos support `NUKE_GIT_USER` / `NUKE_GIT_PASSWORD` environment variables.
- **Transitive Git Deps**: Git dependencies that declare their own `:git-dependencies` are resolved recursively with cycle detection.
- **Cache Management**: New `nuke clean-git-deps` task to wipe the global `~/.nuke/git-deps/` cache.
- **IDE Integration**: IntelliJ plugin now correctly resolves git dependency jars for code completion and compilation.
- **Bug Fix**: Fixed `build-dep-jar` jar packaging — classes were nested under an extra `classes/` prefix.
- **Dependencies Tree**: New `nuke dependencies` task to print a recursive tree of all local, Maven, and Git dependencies.

### v1.1.0
- **Static Analysis Dashboard**: Introduced the `nuke analyze` command to generate a unified `nuke-analysis.html` static analysis dashboard.
- **JaCoCo Coverage**: Added the `nuke metrics` and `nuke test-cov` commands to compute test coverage dynamically and inject it into the dashboard.
- **Error Prone**: Integrated Google's Error Prone directly into the `javac` compile step (enabled via `:error-prone {:enabled true}`).
- **SonarQube CLI**: Integrated seamless SonarScanner execution via the new `nuke sonarqube` task.
- **SpotBugs & PMD**: Bundled static analysis checks that automatically run during `analyze`.
- **Checkstyle**: Introduced unified style checking linked to the dashboard.
- **Nexus IQ**: Added support for detecting and displaying Nexus IQ dependency vulnerabilities in the static analysis dashboard.
- Fixed `uberjar` manifest generation when no `:main-class` is provided.

### v1.0.1
- Integrated basic Nuke build templating via `:templates`.
- Ignored `resources/bin` during standard Git tracking.

### v1.0.0
- Initial open-source release of the Nuke Build Tool.
- Features EDN configuration, built-in Java build tasks, Maven dependency resolution, and custom Coni script tasks.
