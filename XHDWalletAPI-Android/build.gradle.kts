plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    `maven-publish`
    signing
}

group = "app.perawallet.xhdwalletapi"
version = project.property("version") as String

base {
    archivesName.set("XHDWalletAPI-Android")
}

android {
    namespace = "app.perawallet.xhdwalletapi"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 26
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

dependencies {
    api(project(":sharedModule"))
    api(fileTree("../sharedModule/libs") { include("*.jar") })
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

            groupId = project.group.toString()
            artifactId = "xhdwalletapi-android"
            version = project.version.toString()

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
    if (System.getenv("GPG_PRIVATE_KEY") != null) {
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PRIVATE_KEY_PASSWORD")
        )
        sign(publishing.publications)
    }
}