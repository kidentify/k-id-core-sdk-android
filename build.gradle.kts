import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.maven.publish) apply false
}

subprojects {
    // AGP's own javadoc task can't read sealed classes, so the javadoc jar Central requires comes from Dokka.
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<DokkaExtension> {
            dokkaSourceSets.configureEach { suppressAnnotatedWith.add("com.kid.core.KIDInternalApi") }
        }
        val javadocJar = tasks.register<Jar>("dokkaJavadocJar") {
            archiveClassifier.set("javadoc")
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            from(tasks.named("dokkaGeneratePublicationHtml"))
        }
        plugins.withId("com.vanniktech.maven.publish") {
            afterEvaluate {
                extensions.configure<PublishingExtension> {
                    publications.withType<MavenPublication>().configureEach { artifact(javadocJar) }
                }
            }
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            // Central rejects unsigned artifacts; local packing runs without the release key.
            if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
            pom {
                url.set("https://github.com/kidentify/k-id-core-sdk-android")
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("k-id")
                        name.set("k-ID")
                        email.set("support@k-id.com")
                        url.set("https://k-id.com")
                    }
                }
                scm {
                    url.set("https://github.com/kidentify/k-id-core-sdk-android")
                    connection.set("scm:git:https://github.com/kidentify/k-id-core-sdk-android.git")
                    developerConnection.set("scm:git:ssh://git@github.com/kidentify/k-id-core-sdk-android.git")
                }
            }
        }
    }
}
