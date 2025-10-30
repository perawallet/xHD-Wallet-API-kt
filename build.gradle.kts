plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("com.android.library") version "8.13.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
    id("com.gradleup.nmcp.aggregation") version "1.2.0"
}

allprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

nmcpAggregation {
    centralPortal {
        username = System.getenv("CENTRAL_PORTAL_USERNAME")
        password = System.getenv("CENTRAL_PORTAL_PASSWORD")
        publishingType = "AUTOMATIC"
    }

    publishAllProjectsProbablyBreakingProjectIsolation()
}

subprojects {
    plugins.withId("maven-publish") {
        project.pluginManager.apply("signing")

        project.extensions.configure<SigningExtension> {
            val publishing = project.extensions.findByType(PublishingExtension::class.java)
            val key = System.getenv("GPG_PRIVATE_KEY")
            val password = System.getenv("GPG_PRIVATE_KEY_PASSWORD")

            if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
                useInMemoryPgpKeys(key, password)
                sign(publishing?.publications)
            }
        }
    }
}