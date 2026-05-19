# example-custom-plugins

A showcase of all Nuke plugin patterns — custom tasks using `:cmds`, `:coni` scripts, `:deps`, and `:extends`.

## Running Tasks

```sh
# List all available tasks
nuke tasks

# ── Developer Utilities ─────────────────────────────────────────────────
nuke sloc           # Count lines of Java source
nuke dep-audit      # List all jars in libs/
nuke lint           # Run Checkstyle (requires checkstyle.jar in libs/)
nuke format         # Auto-format sources (requires google-java-format.jar in libs/)

# ── Release & Packaging ─────────────────────────────────────────────────
nuke changelog      # Generate CHANGELOG.md from git log
nuke bump           # Bump patch version in nuke.edn (1.0.0 → 1.0.1)
nuke docker         # Build a Docker image (requires Dockerfile)

# ── Deployment ──────────────────────────────────────────────────────────
nuke deploy-ssh     # SCP uberjar to a remote server (configure host first)
nuke github-release # Create a GitHub release via gh CLI

# ── Reporting ───────────────────────────────────────────────────────────
nuke report         # Run tests and print a summary

# ── Workflow Orchestration ───────────────────────────────────────────────
nuke ci             # Full pipeline: clean → test → jar
nuke install-hooks  # Install a git pre-commit hook to run lint
nuke watch          # Watch src/ and recompile on change (requires fswatch)
```

## Plugin Patterns Used

| Task             | Pattern                         |
|------------------|---------------------------------|
| `:sloc`          | `:cmds` — shell commands        |
| `:dep-audit`     | `:cmds` — shell commands        |
| `:lint`          | `:deps` + `:cmds`               |
| `:format`        | `:cmds`                         |
| `:changelog`     | `:cmds`                         |
| `:bump`          | `:coni` — external Coni script  |
| `:docker`        | `:deps` + `:cmds`               |
| `:deploy-ssh`    | `:deps` + `:cmds`               |
| `:github-release`| `:deps` + `:cmds`               |
| `:report`        | `:deps` + `:coni` script        |
| `:ci`            | `:deps` + inline `:coni`        |
| `:install-hooks` | `:cmds`                         |
| `:watch`         | `:cmds`                         |

## Directory Structure

```
example-custom-plugins/
├── nuke.edn                     # All plugin task definitions
├── README.md
├── scripts/
│   ├── bump_version.coni        # Patch version bumper
│   └── coverage_report.coni    # Test summary reporter
└── src/
    └── main/
        └── com/example/
            └── Main.java
```

## Key Concept: `@global-task-config`

Coni scripts (`:coni`) have access to the full parsed `nuke.edn` config via the `@global-task-config` atom:

```clojure
(println "Building:" (:name @global-task-config))
(println "Version:"  (:version @global-task-config))
```
