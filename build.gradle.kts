plugins {
    java
}

group = "wtf.fpp"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(
        files(
            "$rootDir/../fake-player-plugin/build/classes/java/main",
            fileTree("$rootDir/../fake-player-plugin/build/libs") { include("*.jar") }
        )
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveFileName.set("FPP-PvP-${version}.jar")

    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "FPP-PvP",
                "Implementation-Vendor" to "FPP Community",
                "Implementation-Version" to version
            )
        )
    }
}
