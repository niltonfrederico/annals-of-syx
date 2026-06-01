plugins {
    application
    id("com.gradleup.shadow") version "9.0.0"
}

repositories {
    mavenCentral()
}

val gameJar = System.getenv("SOS_GAME_JAR")
    ?: "${rootProject.projectDir}/.source/game/SongsOfSyx.jar"

dependencies {
    implementation(files(gameJar))
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("info.picocli:picocli:4.7.6")
    implementation("org.jline:jline:3.25.1")
    implementation("me.tongfei:progressbar:0.10.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.jimfs:jimfs:1.3.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("dumper.cli.Syx")
}

tasks.named<JavaExec>("run") {
    // PATHS.init() reads ./base/{data.zip,locale.zip,icons,script} from CWD.
    // .source/game/ is the workspace-local game copy; populate via GameLocator.
    // Override with SOS_GAME_DIR env var to point at a different install.
    workingDir = file(System.getenv("SOS_GAME_DIR") ?: "${rootProject.projectDir}/.source/game")
}

tasks.test {
    useJUnitPlatform()
    // Engine-bound integration tests live under dumper.* (PopulationTest,
    // BootstrapProbe, etc.) and require xvfb-run + a real save. Per
    // CLAUDE.md they run via scripts/run.sh, not `./gradlew test`.
    // Keep gradle test green by limiting it to the engine-free CLI suite.
    filter {
        includeTestsMatching("dumper.cli.*")
    }
}
