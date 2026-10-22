plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":engine"))
}

application {
    mainClass.set("dots.cli.MainKt")
}
