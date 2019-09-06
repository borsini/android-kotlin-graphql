import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.40"
}

group = "com.borsini"
version = "1.0-SNAPSHOT"

object Versions {
    const val KTOR_VERSION = "1.2.3"
    const val GRAPH_QL_JAVA_VERSION = "13.0"
}

repositories {
    mavenCentral()
    jcenter()
}


dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("com.graphql-java", "graphql-java", Versions.GRAPH_QL_JAVA_VERSION)
    compile("io.ktor", "ktor-server-core", Versions.KTOR_VERSION)
    compile("io.ktor", "ktor-server-netty", Versions.KTOR_VERSION)
    compile("io.ktor", "ktor-locations", Versions.KTOR_VERSION)
    compile("io.ktor", "ktor-gson", Versions.KTOR_VERSION)
    compile("ch.qos.logback:logback-classic:1.2.3")

    //Test
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}