plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.diffplug.spotless") version "6.16.0"
}

version = "0.0.1"
description = "A gradle plugin generating oss fuzz targets"
val repoUrl = "github.com/loosebazoka/ossfuzz-gradle-plugin"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.bcel:bcel:6.1")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.8.2")
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project)
            }

            targets {
                all {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}

gradlePlugin {
    val ossfuzz by plugins.creating {
        id = "com.loosebazooka.ossfuzz"
        implementationClass = "com.loosebazooka.ossfuzz.OssFuzzPlugin"
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts") // default target for kotlinGradle
        ktlint()
    }
    format("misc") {
        target("*.md", ".gitignore", "**/*.yaml")

        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    java {
        googleJavaFormat("1.6")
        licenseHeaderFile("$rootDir/config/licenseHeader")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.loosebazooka"
            artifactId = "ossfuzz-gradle-plugin"
            version = "1.1"

            from(components["java"])
            pom {
                name.set(project.name)
                description.set(project.description)
                inceptionYear.set("2023")
                url.set(repoUrl)
                organization {
                    name.set("loosebazooka temp")
                    url.set("https://github.com/loosebazooka")
                }
                developers {
                    developer {
                        organization.set("loosebazooka temp")
                        organizationUrl.set("https://github.com/loosebazooka")
                    }
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("$repoUrl/issues")
                }
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:$repoUrl.git")
                    developerConnection.set("scm:git:$repoUrl.git")
                    url.set(repoUrl)
                    tag.set("HEAD")
                }
            }
        }
    }
}
