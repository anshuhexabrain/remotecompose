plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

application {
    mainClass.set("com.example.remotecompose.server.MainKt")
}

dependencies {
    implementation("androidx.compose.remote:remote-core:1.0.0-alpha05")
    implementation("androidx.compose.remote:remote-creation:1.0.0-alpha05")
    implementation("androidx.compose.remote:remote-creation-core:1.0.0-alpha05")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // Shared models are inlined since the shared library doesn't have a JVM target yet
    implementation("androidx.annotation:annotation:1.9.1")
}

kotlin {
    jvmToolchain(19)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.remotecompose.server.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
