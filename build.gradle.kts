import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.attributes.Usage

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("io.freefair.lombok") version "9.1.0"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("io.sentry.jvm.gradle") version "5.12.2"
    `maven-publish`
}

group = "com.mypetclassic"

val buildType = project.findProperty("buildType")?.toString() ?: "local"
val minecraftVersion by extra("1.21.5")
val bukkitPackets by extra("v1_21_R4;v1_21_R5;v1_21_R6;v1_21_R7")
val specialVersions by extra("")

version = "1.0.0"

@Suppress("UNCHECKED_CAST")
val nmsModules: List<String> = gradle.extra["nmsModules"] as List<String>

// Shared repositories for all projects (root + subprojects)
allprojects {
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://repo.userderezzed.dev/releases")
        maven("https://repo.userderezzed.dev/snapshots")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "io.sentry.jvm.gradle")

    repositories {
        maven("https://mvn.lib.co.nz/spigot/")
        maven("https://repo.md-5.net/content/groups/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.md-5.net/content/repositories/public")
        maven("https://jitpack.io")
    }

    tasks.named("processResources") { enabled = false }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    if (project.name != "api") {
        plugins.withId("maven-publish") {
            tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
            tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

val archivesBaseName = "MyPetClassic"
val versionSuffix = when (buildType) {
    "release" -> ""
    "snapshot", "dev" -> "-SNAPSHOT"
    else -> "-SNAPSHOT-local"  // local builds
}
val fullVersion = "${project.version}$versionSuffix"

sourceSets {
    main {
        resources { srcDir("src/main/resources/") }
    }
}

val downloadTranslations by tasks.register<Exec>("downloadTranslations") {
    group = "resources"
    description = "Downloads MyPet translations into build/resources/main/locale"
    val targetDir = layout.buildDirectory.dir("resources/main/locale").get().asFile
    outputs.dir(targetDir)

    // Skip download if translations are less than 12 hours old
    val maxAgeMillis = 12 * 60 * 60 * 1000L // 12 hours in milliseconds
    onlyIf {
        if (!targetDir.exists()) {
            true
        } else {
            val ageMillis = System.currentTimeMillis() - targetDir.lastModified()
            val shouldDownload = ageMillis > maxAgeMillis
            if (!shouldDownload) {
                logger.lifecycle("Skipping translation download - last updated ${ageMillis / (60 * 60 * 1000)} hours ago (max: 12 hours)")
            }
            shouldDownload
        }
    }

    doFirst {
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
    }
    commandLine(
        "git", "clone", "--depth", "1", "--single-branch",
        "https://github.com/MyPetORG/MyPet-Translations.git", targetDir
    )
    doLast {
        // Clean up files we don't need in the final JAR
        // Use direct File operations for configuration cache compatibility
        File(targetDir, ".git").deleteRecursively()
        File(targetDir, ".gitignore").delete()
        File(targetDir, "README.md").delete()
        File(targetDir, "exclude").deleteRecursively()
        // The translation repository is not part of the plugin: do not ship its Crowdin/CI metadata.
        File(targetDir, ".github").deleteRecursively()
        File(targetDir, "crowdin.yml").delete()
        File(targetDir, "crowdin.yaml").delete()
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(downloadTranslations)
    duplicatesStrategy = DuplicatesStrategy.WARN

    // Define filtering properties inline for configuration cache compatibility
    val filteringProps = mapOf(
        "buildNumber" to (providers.gradleProperty("BUILD_NUMBER").orNull ?: ""),
        "gitCommit" to (System.getenv("GIT_COMMIT") ?: ""),
        "minecraft" to mapOf("version" to minecraftVersion),
        "bukkit" to mapOf("packets" to bukkitPackets),
        "special" to mapOf("versions" to specialVersions),
        "mypetVersion" to version,
        "timestamp" to DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now()),
    )

    filesMatching("*.yml") {
        filteringCharset = "UTF-8"
        expand(filteringProps)
    }
}

// Only the official CI has this secret. Forks and self-compiled builds get an empty DSN, so
// SentryErrorReporter stays disabled instead of silently reporting a fork's errors to our project.
val sentryDsn = System.getenv("SENTRY_DSN") ?: ""

fun Manifest.attributesForMyPet() = attributes(
    mapOf(
        "Class-Path" to "MyPet/rhino.jar MyPet/rhino-1.7.9.jar MyPet/rhino-1.7.10.jar MyPet/rhino-1.7.15.jar ../MyPet/rhino.jar ../MyPet/rhino-1.7.9.jar ../MyPet/rhino-1.7.10.jar ../MyPet/rhino-1.7.15.jar MyPet/mongo-java-driver.jar MyPet/mongo-java-driver-3.12.11.jar",
        "Main-Class" to "de.Keyle.MyPet.Main",
        "Project-Name" to project.name,
        "Project-Version" to fullVersion,
        "Project-Build" to (System.getenv("BUILD_NUMBER") ?: ""),
        "Project-Type" to buildType,
        "Project-Minecraft-Version" to minecraftVersion,
        "Project-Bukkit-Packets" to bukkitPackets,
        "Special-MC-Versions" to specialVersions,
        "Git-Commit" to (System.getenv("GIT_COMMIT") ?: ""),
        "Sentry-Dsn" to sentryDsn,
        // The NMS modules are compiled against the Mojang-mapped paperweight dev bundle. Without this
        // marker Paper treats every plugin.yml plugin as Spigot-mapped and remaps it on startup, which
        // rewrites correct Mojang references (e.g. Registry.register -> IdMap.register) and breaks NMS.
        "paperweight-mappings-namespace" to "mojang"
    )
)

tasks.named<Jar>("jar") {
    archiveBaseName.set(archivesBaseName)
    archiveFileName.set("${archivesBaseName}-${fullVersion}.jar")
    archiveVersion.set(fullVersion)
    manifest { attributesForMyPet() }
}

/* ---------- Shading without JVM attribute conflicts ---------- */

// Create a resolvable-only configuration to collect jars to shade.
val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // Mark as runtime usage and leave TargetJvmVersion unset.
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}

dependencies {
    // Pull submodules as built jars (regardless of their target JVM)
    add("shade", project(path = ":plugin", configuration = "runtimeElements"))
    add("shade", project(path = ":api",    configuration = "runtimeElements"))
    add("shade", project(path = ":skills", configuration = "runtimeElements"))
    nmsModules.forEach { add("shade", project(path = it, configuration = "runtimeElements")) }

    // External libs to be shaded

    add("shade", "org.bstats:bstats-bukkit:3.1.0")
    add("shade", "org.mongodb:mongodb-driver:3.12.11")
    add("shade", "de.keyle:knbt:0.0.6")
    add("shade", "com.google.code.gson:gson:2.8.9")
    add("shade", "io.sentry:sentry:8.22.0")
    // sentry-logback is not shaded: Paper logs through log4j2, so the logback integration is dead weight.
    add("shade", "com.zaxxer:HikariCP:3.4.2")
}

// Build the shaded jar strictly from the 'shade' configuration
val shadowJar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set(archivesBaseName)
    archiveVersion.set(fullVersion)
    archiveClassifier.set("")
    // Keep META-INF/services (ServiceLoader) and third-party licences, but drop signatures and the
    // SLF4J API: the server already provides SLF4J 2.x, and bundling 1.7 classes shadows it.
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("org/slf4j/**")
    manifest { attributesForMyPet() }

    dependsOn(shade)

    configurations = listOf(shade)


    relocate("org.bstats", "de.Keyle.MyPet.util.metrics")
    relocate("com.zaxxer.hikari", "de.Keyle.MyPet.util.hikari")
    relocate("de.keyle.knbt", "de.Keyle.MyPet.util.nbt")
    relocate("org.bson", "de.Keyle.MyPet.util.bson")
    relocate("com.google.gson", "de.Keyle.MyPet.util.gson")
    relocate("com.mongodb", "de.Keyle.MyPet.util.mongodb")
    relocate("io.sentry", "de.Keyle.MyPet.util.sentry")
}

tasks.named("assemble") { dependsOn(shadowJar) }
tasks.named("build") { dependsOn(shadowJar) }

/* ---------- Sentry Configuration ---------- */

sentry {
    // Only bundle sources when auth token is available (snapshot/release builds)
    // PR builds don't have the token and don't need source bundling
    includeSourceContext = !System.getenv("SENTRY_AUTH_TOKEN").isNullOrEmpty()

    org = "mypet"
    projectName = "mypet"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")

    // The actual code lives in the api/plugin/nms subprojects, but the shipped artifact is a single
    // shaded jar built here at the root with ONE sentry-debug-meta.properties. Enabling source
    // context per-subproject would emit colliding debug-meta files that shadowJar silently dedups,
    // so instead we fold every module's source root into this project's single source bundle. One
    // bundle id, one properties file, all sources -> stack frames across all modules symbolicate.
    val moduleSourceDirs = (listOf("api", "plugin") +
        file("nms").listFiles().orEmpty()
            .filter { it.isDirectory && File(it, "src/main/java").isDirectory }
            .map { "nms/${it.name}" })
        .map { "$it/src/main/java" }
        .filter { file(it).isDirectory }
    additionalSourceDirsForSourceContext.set(moduleSourceDirs)
}

/* ---------- Root compilation settings (Java 21 output) ---------- */

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

/* ---------- Hangar Release ---------- */

hangarPublish {
    publications.register("plugin") {
        val hangarVersion = providers.gradleProperty("HANGAR_VERSION").orNull
            ?: project.version.toString()
        val hangarFile = providers.gradleProperty("HANGAR_FILE").orNull
            ?: "build/libs/MyPet-${fullVersion}.jar"
        val hangarChangelog = providers.gradleProperty("HANGAR_CHANGELOG").orNull
            ?: "View the full changelog on Modrinth: https://modrinth.com/plugin/mypet/version/$hangarVersion"

        version.set(hangarVersion)
        id.set("MyPetClassic")
        channel.set(if (buildType == "release") "Release" else "Snapshot")
        changelog.set(hangarChangelog)
        apiKey.set(
            providers.gradleProperty("HANGAR_TOKEN").orNull
                ?: System.getenv("HANGAR_TOKEN")
                ?: ""
        )

        platforms {
            paper {
                jar.set(file(hangarFile))
                // Only the Minecraft versions that actually have a compat module in this jar.
                platformVersions.set(listOf(
                    "1.21.5", "1.21.6", "1.21.7", "1.21.8",
                    "1.21.9", "1.21.10", "1.21.11"
                ))
            }
        }
    }
}
