plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

apply(from = rootProject.file("nms/nmsModule.gradle"))

extra["craftbukkitVersion"] = "1.21.8-R0.1-SNAPSHOT"

description = "v1_21_R5"

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}
