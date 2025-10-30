plugins {
    `java-library`
    `maven-publish`
    signing
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

sourceSets {
    named("main") {
        resources {
            srcDirs("src/main/resources", "src/main/jniLibs")
        }
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

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "lazysodium-java"
            version = project.version.toString()

            pom {
                name.set("Lazysodium Java")
                description.set(project.description)
                url.set("https://github.com/terl/lazysodium-java")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("goterl")
                        name.set("Goterl Team")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/terl/lazysodium-java.git")
                    developerConnection.set("scm:git:ssh://github.com/terl/lazysodium-java.git")
                    url.set("https://github.com/terl/lazysodium-java")
                }
            }
        }
    }
}

signing {
    if (System.getenv("GPG_PRIVATE_KEY") != null) {
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PRIVATE_KEY_PASSWORD")
        )
        sign(publishing.publications)
    }
}