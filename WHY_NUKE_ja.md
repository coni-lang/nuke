# MavenとGradleに人生を浪費するのは、もうやめろ。

ただ動くビルドツールがある。

---

## 誰も語らない問題

あなたがJava開発者になったのは、ものを作るためだ。

なのに午前中は800行の `pom.xml` と格闘している。午後はGradleが何かを「設定」するのに45秒かかり、コンパイルすら始まらないのを待っている。夜はチームの誰も理解していないXMLの定型文をコピペしている。

これは開発じゃない。これは苦行だ。

---

## Nukeを紹介する。

**バイナリ1つ。設定ファイル1つ。XMLゼロ。**

```sh
brew install nuke   # またはバイナリをコピーするだけ
nuke run            # 以上
```

JVM起動のオーバーヘッドなし。プラグインエコシステムの迷路なし。突然再起動を決める「デーモン」なし。47ステップの「はじめに」ガイドなし。

Javaプロジェクトを正確に、毎回、ミリ秒単位でビルドするネイティブバイナリだけがある。

---

## 数字は嘘をつかない

| | Maven | Gradle | **Nuke** |
|---|---|---|---|
| 設定ファイルサイズ（Hello World） | ~40行のXML | ~20行のGroovy/Kotlin | **5行のEDN** |
| 初回ビルド起動時間 | ~4〜8秒 | ~6〜12秒 | **< 0.1秒** |
| 設定形式 | XML | Groovy / Kotlin DSL | **EDN（コードではなくデータ）** |
| インストールサイズ | ~10 MB + JVM | ~130 MB + JVM | **~20 MB 自己完結** |
| ビルドツール実行にJVM必要 | ✅ 必要 | ✅ 必要 | **❌ 不要** |
| エアギャップ / オフラインサポート | 複雑 | 複雑 | **`nuke mirror export`** |
| ローカルマルチモジュール依存 | 冗長 | 冗長 | **`:local-dependencies`** |
| 公開なしのGit依存関係 | ❌ 不可 | プラグイン必要 | **`:git-dependencies`** |
| 学習曲線 | 数週間 | 数週間 | **30分** |

---

## 本物のビルドとはこういうものだ

### Mavenはこう言う：

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
        <!-- fat jarを作るためにさらに40行 -->
      </plugin>
    </plugins>
  </build>
</project>
```

### Nukeはこう言う：

```edn
{:name "my-app"
 :version "1.0.0"
 :dependencies ["org.apache.commons:commons-lang3:3.12.0"]
 :main-class "com.example.Main"}
```

**同じ結果。92%少ないタイピング。**

---

## 本当に重要な機能

### ✅ ただ動くローカル依存関係

マルチモジュールプロジェクトを構築している？パスで兄弟プロジェクトを参照すれば、Nukeが正しい順序で自動的にビルドする。

```edn
:local-dependencies ["../core-lib" "../utils-lib"]
```

`mvn install` の踊りなし。`settings.gradle` の宣言なし。ただパスを書くだけ。

### ✅ 苦痛のないGit依存関係

Nexusへの公開なしに、Gitリポジトリから直接ライブラリを取り込める。

```edn
:git-dependencies ["https://github.com/myorg/mylib.git//core#v2.1.0"]
```

### ✅ ビルトインJUnit 5サポート

JUnit 5はそのまま動く。Surefireプラグインなし。設定なし。テストを書くだけ。

```sh
nuke test
```

### ✅ 3行でカスタムタスク

```edn
:tasks {:deploy-prod {:extends "uberjar"
                      :jar-name "out/prod.jar"
                      :desc "リリースする"}}
```

### ✅ エアギャップ / オフラインビルド

コマンド1つでプロジェクトに必要なもの全て——Nukeのビルトインツールも含めて——をポータブルなzipにまとめる。エアギャップサーバーにコピーして、そのままビルドできる。

```sh
nuke mirror export release-mirror.zip
# エアギャップマシンに転送
nuke mirror import release-mirror.zip
nuke uberjar  # オフラインでも完璧に動作
```

### ✅ 依存関係分析

クラスパスにある全jarではなく、コードが実際に使っているjarを正確に把握する。

```sh
nuke analyze-deps html   # 美しいHTMLレポート
```

### ✅ テンプレート

ビルド時にバージョン文字列、ビルドタイムスタンプ、環境名を設定ファイルに埋め込む。プラグインなし。コードなし。

```
app.version=${version}   →   app.version=1.0.0
```

### ✅ 面倒なしのJAXB

XSDスキーマからJavaを生成して出力を自動的にパッチ——すべて設定ファイルで宣言するだけ。

---

## 「でも心配なのは...」

**「Nukeにない機能が必要になったら？」**
Nukeは実際のJavaプロジェクトが必要とするものの95%をカバーしている。残りの5%は、カスタムタスクから任意のシェルコマンドを呼び出せる。

**「CI/CDパイプラインはどうなる？」**
Nukeは単一バイナリだ。どこにでも置ける。依存関係ゼロでmacOS、Linux、Windowsで動く。

**「IDEサポートは？」**
IntelliJプラグインがある。そして、NukeはMavenと同じ `~/.m2/repository` を使うため、IDEのMaven連携も引き続き機能する。

**「Spring Bootは？」**
Spring BootはNukeで問題なく動く。Spring Bootの依存関係を追加して、`nuke uberjar` を実行するだけ。以上。

**「チームに200人のMavenユーザーがいる。」**
MavenはMavenのままでいい。Nukeは共存できる。新しいサービス1つから始めよう。ビルドツールの幸せとはどういうものかを体験しよう。それから決めればいい。

---

## MavenとGradleの本当のコスト

チームがMavenやGradleを使う毎日、こんな代償を払っている：

- 開発者1人あたり1日約**2分**のビルドツール起動待ち
- Gradleプラグイン設定の `NullPointerException` デバッグに週約**30分**
- 新しい開発者のビルドシステムへのオンボーディングに四半期あたり約**1日**
- 実際のソフトウェアではなく、ソフトウェアをビルドするためのツールに費やす**無数の時間**

10人の開発チームなら、年間**何週間もの工数**をビルドツールに費やしている。製品ではなく。

**Nukeはその時間を返す。**

---

## 今すぐ試す

```sh
# リリースページからプラットフォーム用バイナリをダウンロード
chmod +x nuke-mac && mv nuke-mac /usr/local/bin/nuke

# プロジェクトにnuke.ednを作成
echo '{:name "my-project" :version "1.0.0" :main-class "com.example.Main"}' > nuke.edn

# ビルド
nuke run
```

以上だ。インストールウィザードなし。アカウント登録なし。「ウォームアップ」なし。

**コードだけが、ビルドされる。**

---

> *「600行のpom.xmlを削除して8行のnuke.ednに置き換えた。チームのビルド時間は40秒から3秒になった。後悔はしていない。」*

---

## Nukeを入手

📦 **ダウンロード:** [github.com/coni-lang/nuke/releases](https://github.com/coni-lang/nuke/releases)  
📖 **チュートリアル:** [TUTORIAL_ja.md](./TUTORIAL_ja.md)  
🔌 **IntelliJプラグイン:** リリースzipに同梱

**ビルドツールは、あなたの仕事で一番難しい部分であるべきではない。**

Nukeは、それを一番簡単な部分にする。
