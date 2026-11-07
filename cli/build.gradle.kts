plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":engine"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("dots.cli.MainKt")
}

// AssetRegisterTest checks docs/assets.md against the files in the repository. Declaring them as
// inputs makes the tests run again when either changes, instead of counting as up to date. Full
// failure messages put the list of problems (for example an unregistered file) in the CI log.
tasks.test {
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    inputs.files(
        fileTree(rootDir) { exclude("**/build/**", ".git/**", ".gradle/**", ".kotlin/**", ".idea/**") },
    ).withPropertyName("repositoryFiles").withPathSensitivity(PathSensitivity.RELATIVE)
}
