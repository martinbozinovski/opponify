plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":modules:identity")); implementation(project(":modules:player")); implementation(project(":modules:team")); implementation(project(":modules:sport"))
    implementation(project(":modules:facility")); implementation(project(":modules:opportunity")); implementation(project(":modules:participation")); implementation(project(":modules:game"))
    implementation(project(":modules:scheduling")); implementation(project(":modules:attendance")); implementation(project(":modules:results")); implementation(project(":modules:history"))
    implementation(project(":modules:trust")); implementation(project(":modules:moderation")); implementation(project(":modules:notification"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("software.amazon.awssdk:sqs:2.34.7")
    implementation("com.google.firebase:firebase-admin:9.5.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") }
}
