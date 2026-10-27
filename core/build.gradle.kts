import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.kid.core"
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
    api(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.age.signals)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(variant = "release", sourcesJar = true, publishJavadocJar = false),
    )
    coordinates(artifactId = "core")
    pom {
        name.set("k-ID Core")
        description.set("Age assurance and parental consent for Android apps, by k-ID.")
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
