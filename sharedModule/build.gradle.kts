import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    `java-library`
    `maven-publish`
    signing
}

group = "app.perawallet.xhdwalletapi"
version = project.property("version") as String

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib"))
    api("commons-codec:commons-codec:1.19.0")
    api("net.java.dev.jna:jna:5.18.1")
    api("com.goterl:resource-loader:2.1.0")
    api("cash.z.ecc.android:kotlin-bip39:1.0.9")
    api("net.pwall.json:json-kotlin-schema:0.57")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    api("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    api("com.fasterxml.jackson.core:jackson-core:2.20.0")
    api("org.msgpack:jackson-dataformat-msgpack:0.9.10")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.20.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "xhdwalletapi-shared"
            version = project.version.toString()
            pom {
                name.set("XHDWalletAPI Shared")
                description.set("Shared Kotlin/Java module for XHDWalletAPI containing core utilities and serialization support.")
                url.set("https://github.com/perawallet/xHDWalletAPI-Android")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("perawallet")
                        name.set("Pera Wallet")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/perawallet/xHDWalletAPI-Android.git")
                    developerConnection.set("scm:git:ssh://github.com/perawallet/xHDWalletAPI-Android.git")
                    url.set("https://github.com/perawallet/xHDWalletAPI-Android")
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