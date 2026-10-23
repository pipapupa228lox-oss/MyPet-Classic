rootProject.name = "MyPetClassic"

include("api", "plugin", "skills")

val nmsModules = File(rootDir, "nms").listFiles()!!.filter { s -> s.isDirectory() && s.name.matches("v[\\d_]+R\\d+".toRegex()) }.map { s -> ":nms:" + s.name }
    // v26 modules need private spigot artifacts with no public dev-bundle; they stay on disk but out of the build
    .filter { !it.startsWith(":nms:v26_") }
gradle.extra["nmsModules"] = nmsModules
include(nmsModules)
