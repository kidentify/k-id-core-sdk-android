import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.kid.core.ui"
    compileSdk = 36
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        // Versions are pinned on purpose; these only report that newer ones exist.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
        optIn.add("com.kid.core.KIDInternalApi")
    }
}

dependencies {
    api(project(":core"))
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(variant = "release", sourcesJar = true, publishJavadocJar = false),
    )
    coordinates(artifactId = "core-ui")
    pom {
        name.set("k-ID Core UI")
        description.set("Jetpack Compose age gate, consent and settings screens for Android apps, by k-ID.")
    }
}

publishing {
    repositories {
        maven {
            name = "stage"
            url = uri(providers.gradleProperty("stageRepo").getOrElse(layout.buildDirectory.dir("stage").get().asFile.path))
        }
    }
}
