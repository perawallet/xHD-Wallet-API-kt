plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.gradleup.nmcp")
    `maven-publish`
    signing
}


base {
    archivesName.set("xhdwalletapi-android")
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

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    api(project(":lazysodium-java"))
    api("commons-codec:commons-codec:1.19.0")
    api("cash.z.ecc.android:kotlin-bip39:1.0.9")
    api("net.pwall.json:json-kotlin-schema:0.57")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    api("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    api("com.fasterxml.jackson.core:jackson-core:2.20.0")
    api("org.msgpack:jackson-dataformat-msgpack:0.9.10")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.20.0")
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

            groupId = "app.perawallet.xhdwalletapi"
            artifactId = "xhdwalletapi-android"
            version = "1.2.2"

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
    val password = System.getenv("GPG_PRIVATE_KEY_PASSWORD")

    if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }
}