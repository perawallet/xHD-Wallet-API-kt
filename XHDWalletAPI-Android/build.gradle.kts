import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.signing)
    alias(libs.plugins.nmcp)
}

group = "app.perawallet"
version = libs.versions.library.version.get()

base {
    archivesName.set("xhdwalletapi-android")
}

android {
    namespace = "app.perawallet.xhdwalletapi"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = libs.versions.android.ndk.get()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64") }
    }

    buildFeatures { viewBinding = true }

    lint { baseline = file("lint-baseline.xml") }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api("net.java.dev.jna:jna:5.18.1@aar")
    implementation(libs.resource.loader)
    implementation(libs.slf4j.api)
    implementation(libs.commons.codec)
    implementation(libs.bip39)
    implementation(libs.json.kotlin.schema)
    implementation(libs.serialization.json)
    implementation(libs.serialization.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.core)
    implementation(libs.jackson.dataformat.msgpack)
    implementation(libs.jackson.dataformat.cbor)
}

tasks.register<Copy>("copyAarToRoot") {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/aar"))
    into(rootDir.resolve("build"))
    include("*.aar")
}

tasks.named("build") {
    finalizedBy("copyAarToRoot")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }

            groupId = group.toString()
            artifactId = "xhdwalletapi-android"

            pom {
                name.set("XHDWalletAPI-Android")
                description.set("Android implementation of the extended hierarchical deterministic wallet API for Algorand, part of the open-source Pera Wallet ecosystem.")
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
    val key = System.getenv("GPG_PRIVATE_KEY")
    val password = System.getenv("GPG_PASSPHRASE")

    if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = System.getenv("CENTRAL_PORTAL_USERNAME")
        password = System.getenv("CENTRAL_PORTAL_PASSWORD")
        publishingType = "AUTOMATIC"
    }
}