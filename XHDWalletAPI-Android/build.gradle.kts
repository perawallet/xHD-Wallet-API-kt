plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("maven-publish")
    id("signing")
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
        targetSdk = 36
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        baseline = file("lint-baseline.xml")
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

val androidSourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
    from(android.sourceSets["main"].res.srcDirs)
}

publishing {
    publications {
        create<MavenPublication>("mavenAndroid") {
            groupId = project.group.toString()
            artifactId = "xhdwalletapi-android"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            artifact(androidSourcesJar.get())

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

tasks.matching { it.name == "generateMetadataFileForMavenAndroidPublication" }
    .configureEach { dependsOn(androidSourcesJar) }

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PRIVATE_KEY_PASSWORD")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["mavenAndroid"])
    }
}