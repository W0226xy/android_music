plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "com.example"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-cors:2.3.12")
    implementation("io.ktor:ktor-server-host-common:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-server-config-yaml:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.4.8")
    testImplementation("io.ktor:ktor-server-tests:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.10")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.music.server.ApplicationKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(" ") { it.name }
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
