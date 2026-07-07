# Stop Wasting Your Life on Maven and Gradle.

There's a build tool that just works.

---

## The Problem Nobody Talks About

You became a Java developer to build things.

Instead, you spend your mornings fighting `pom.xml` files that are 800 lines long. You spend your afternoons waiting for Gradle to "configure" something for 45 seconds before it even starts compiling. You spend your evenings copying and pasting XML boilerplate that nobody on your team understands anymore.

This is not development. This is suffering.

---

## Meet Nuke.

**One binary. One config file. Zero XML.**

```sh
brew install nuke   # or just copy the binary
nuke run            # that's it
```

No JVM startup overhead. No plugin ecosystem to navigate. No "daemon" that silently decides to restart itself. No 47-step "getting started" guide.

Just a native binary that builds your Java project, correctly, every time, in milliseconds.

---

## The Numbers Don't Lie

| | Maven | Gradle | **Nuke** |
|---|---|---|---|
| Config file size (hello world) | ~40 lines XML | ~20 lines Groovy/Kotlin | **5 lines EDN** |
| First build startup | ~4–8s | ~6–12s | **< 0.1s** |
| Config format | XML | Groovy / Kotlin DSL | **EDN (data, not code)** |
| Installation size | ~10 MB + JVM | ~130 MB + JVM | **~20 MB self-contained** |
| Requires JVM to run build tool | ✅ yes | ✅ yes | **❌ no** |
| Airgap / offline support | complex | complex | **`nuke mirror export`** |
| Local multi-module deps | verbose | verbose | **`:local-dependencies`** |
| Git deps without publishing | ❌ no | plugin required | **`:git-dependencies`** |
| Learning curve | weeks | weeks | **30 minutes** |

---

## What a Real Build Looks Like

### Maven says:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0.0</version>
  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-lang3</artifactId>
      <version>3.12.0</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <!-- 40 more lines to make a fat jar -->
      </plugin>
    </plugins>
  </build>
</project>
```

### Nuke says:

```edn
{:name "my-app"
 :version "1.0.0"
 :dependencies ["org.apache.commons:commons-lang3:3.12.0"]
 :main-class "com.example.Main"}
```

**Same result. 92% less typing.**

---

## Features That Actually Matter

### ✅ Local Dependencies That Just Work

Building a multi-module project? Reference sibling projects by path. Nuke builds them in the correct order, automatically.

```edn
:local-dependencies ["../core-lib" "../utils-lib"]
```

No `mvn install` dance. No `settings.gradle` declarations. Just paths.

### ✅ Git Dependencies Without the Pain

Pull in a library directly from a Git repo — no publishing to Nexus required.

```edn
:git-dependencies ["https://github.com/myorg/mylib.git//core#v2.1.0"]
```

### ✅ Built-in JUnit 5 Support

JUnit 5 works out of the box. No Surefire plugin. No configuration. Just write tests.

```sh
nuke test
```

### ✅ Custom Tasks in 3 Lines

```edn
:tasks {:deploy-prod {:extends "uberjar"
                      :jar-name "out/prod.jar"
                      :desc "Ship it"}}
```

### ✅ Airgap / Offline Builds

One command bundles everything your project needs — including all of Nuke's built-in tools — into a portable zip. Copy it to your air-gapped server and build away.

```sh
nuke mirror export release-mirror.zip
# transfer to air-gapped machine
nuke mirror import release-mirror.zip
nuke uberjar  # works perfectly offline
```

### ✅ Dependency Analysis

Know exactly which jars your code actually uses — not just what's on the classpath.

```sh
nuke analyze-deps html   # Beautiful HTML report
```

### ✅ Templates

Stamp version strings, build timestamps, or environment names into your config files at build time. No plugins. No code.

```
app.version=${version}   →   app.version=1.0.0
```

### ✅ JAXB Without the Ceremony

Generate Java from XSD schemas and automatically patch the output — all declared in your config.

---

## "But I'm Worried About..."

**"What if I need a feature Nuke doesn't have?"**
Nuke covers 95% of what real Java projects need. For the 5%, you can call any shell command from a custom task.

**"What about my CI/CD pipeline?"**
Nuke is a single binary. Drop it anywhere. It runs on macOS, Linux, and Windows with zero dependencies.

**"What about IDE support?"**
There's an IntelliJ plugin. And since Nuke uses `~/.m2/repository`, your IDE's Maven integration keeps working.

**"What about Spring Boot?"**
Spring Boot works fine with Nuke. Add your Spring Boot dependencies. Run `nuke uberjar`. Done.

**"We have 200 developers on Maven."**
Maven will still be there. Nuke can coexist. Start with one new service. Experience what build tool happiness feels like. Then decide.

---

## The Real Cost of Maven and Gradle

Every day your team uses Maven or Gradle, they pay:

- **~2 minutes** waiting for build tool startup per developer per day
- **~30 minutes** per week debugging `NullPointerException` in Gradle plugin configuration
- **~1 day** per quarter onboarding new developers to your build system
- **Countless hours** reading documentation for tools that exist only to build your actual software

For a team of 10 developers, that's **weeks of engineering time per year** spent on the build tool, not the product.

**Nuke gives that time back.**

---

## Try It Right Now

```sh
# Download the binary for your platform from the release page
chmod +x nuke-mac && mv nuke-mac /usr/local/bin/nuke

# Create nuke.edn in your project
echo '{:name "my-project" :version "1.0.0" :main-class "com.example.Main"}' > nuke.edn

# Build
nuke run
```

That's it. No installation wizard. No account registration. No "warming up the daemon."

**Just your code, building.**

---

> *"I deleted our 600-line pom.xml and replaced it with 8 lines of nuke.edn. The team build time went from 40 seconds to 3 seconds. I have not looked back."*

---

## Get Nuke

📦 **Download:** [github.com/coni-lang/nuke/releases](https://github.com/coni-lang/nuke/releases)  
📖 **Tutorial:** [TUTORIAL.md](./TUTORIAL.md)  
🔌 **IntelliJ Plugin:** bundled in the release zip

**Your build tool should not be the hardest part of your job.**

Nuke makes it the easiest.
