# example-java-upload

Example project demonstrating `nuke upload` to a Nexus repository.

## nuke.edn

```edn
{:name "my-app"
 :version "1.0.0"
 :group-id "home.klabs"
 :main-class "home.klabs.Main"
 :deploy "http://nexus.klabs.home/repository/maven-releases/"}
```

## Credentials

Nuke resolves deploy credentials in this order:

### 1. Environment variables (recommended for CI)

```bash
export NUKE_DEPLOY_USER=myuser
export NUKE_DEPLOY_PASSWORD=mypassword
nuke upload
```

### 2. Maven `~/.m2/settings.xml` (recommended for local dev)

Add a `<server>` block with an `<id>` matching your `:deploy-repo` (defaults to `maven-releases`):

```xml
<settings>
  <servers>
    <server>
      <id>maven-releases</id>
      <username>myuser</username>
      <password>mypassword</password>
    </server>
  </servers>
</settings>
```

### 3. Built-in defaults

If neither env vars nor `settings.xml` are found, nuke falls back to `admin` / `lpwesab8`.

## Usage

```bash
cd example-java-upload

# Full pipeline: clean → compile → test → uberjar → zip → upload
nuke upload

# Or run the complete build (includes upload)
nuke build
```
