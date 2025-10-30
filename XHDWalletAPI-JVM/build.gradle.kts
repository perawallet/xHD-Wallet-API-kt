version = project.property("version") as String
group = "app.perawallet.xhdwalletapi"

plugins {
    kotlin("plugin.serialization") version "2.2.21"
    kotlin("jvm")
    `java-library`
    `maven-publish`
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    api(project(":sharedModule"))
    testImplementation("com.algorand:algosdk:2.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.guava:guava:33.5.0-jre")
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("sandbox")
    }
    testLogging.showStandardStreams = true
}

tasks.register<Test>("testWithKhovratovichSafetyDepth") {
    useJUnitPlatform {
        excludeTags("sandbox")
    }
    systemProperty("khovratovichSafetyTest", "true")
    testLogging.showStandardStreams = true
}

tasks.register<Test>("testWithAlgorandSandbox") {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

tasks.jar {
    archiveFileName.set("XHDWalletAPI-JVM.jar")
}

tasks.register<Copy>("copyJarToRoot") {
    dependsOn("assemble")
    from("$buildDir/libs")
    into("$rootDir/build")
    include("*.jar")
}

tasks.named("build") {
    if (!System.getenv("CI").isNullOrEmpty()) return@named
    finalizedBy("copyJarToRoot")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = "xhdwalletapi"
            version = project.version.toString()

            from(components["java"])
            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])

            pom {
                name.set("XHDWalletAPI")
                description.set("A library for extended hierarchical deterministic wallets for JVM.")
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
                        name.set("Pera Wallet Team")
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
