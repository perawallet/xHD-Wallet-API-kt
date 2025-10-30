plugins {
    `java-library`
}

group = "app.perawallet"
version = "5.1.5"
description = "Lazysodium (Java) makes it effortless for Java developers to get started with Libsodium's cryptography."

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/jniLibs") {
        include("**/*.so")
        into("jniLibs")
    }
}

dependencies {
    api("com.goterl:resource-loader:2.1.0")
    api("net.java.dev.jna:jna:5.18.1")
    implementation("org.slf4j:slf4j-api:2.0.17")
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Automatic-Module-Name"] = "app.perawallet.lazysodium"
    }
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
