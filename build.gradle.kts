import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    id("java")
}

group = "de.ragesith"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    compileOnly(files("libs/tinymessage-2.0.1.jar"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// Build number tracking
val buildPropsFile = file("build.properties")
val buildProps = Properties()
if (buildPropsFile.exists()) {
    buildPropsFile.inputStream().use { buildProps.load(it) }
}

// Increment build number
val currentBuildNumber = (buildProps.getProperty("buildNumber", "0")?.toIntOrNull() ?: 0) + 1
buildProps.setProperty("buildNumber", currentBuildNumber.toString())
buildPropsFile.outputStream().use { buildProps.store(it, null) }

// Get git commit hash
fun getGitCommit(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// Get git branch
fun getGitBranch(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// Capture values at configuration time
val projectVersion = version.toString()

// Generate BuildInfo.java task
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val outputDir = file("src/main/java/de/ragesith/hyarena2/generated")
    val outputFile = file("$outputDir/BuildInfo.java")

    outputs.file(outputFile)

    doLast {
        outputDir.mkdirs()

        val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val gitCommit = getGitCommit()
        val gitBranch = getGitBranch()

        outputFile.writeText("""
            |package de.ragesith.hyarena2.generated;
            |
            |/**
            | * Auto-generated build information. DO NOT EDIT MANUALLY.
            | */
            |public final class BuildInfo {
            |    public static final String VERSION = "$projectVersion";
            |    public static final int BUILD_NUMBER = $currentBuildNumber;
            |    public static final String GIT_COMMIT = "$gitCommit";
            |    public static final String GIT_BRANCH = "$gitBranch";
            |    public static final String BUILD_TIME = "$buildTime";
            |
            |    private BuildInfo() {}
            |
            |    public static String getFullVersion() {
            |        return VERSION + " (Build #" + BUILD_NUMBER + ")";
            |    }
            |}
        """.trimMargin())

        println("Generated BuildInfo.java - Version: $projectVersion, Build: $currentBuildNumber")
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateBuildInfo)
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources")
    destinationDirectory.set(file("/home/hypanel/hytale/fdfae6e5-819f-466e-bed3-2feef026eb90/mods"))
}
