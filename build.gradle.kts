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