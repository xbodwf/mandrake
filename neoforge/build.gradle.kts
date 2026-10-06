plugins {
    id("net.neoforged.gradle.userdev")
}

base {
    archivesName.set("${rootProject.findProperty("archives_base_name")}-neoforge${project.findProperty("minecraft_version")}")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

runs {
    configureEach {
        workingDirectory.set(project.layout.projectDirectory.dir("run").dir(name))
        modSource(project.sourceSets.main.get())
    }
    register("client")
    register("server")
}

dependencies {
    implementation("net.neoforged:neoforge:${project.findProperty("neoforge_version")}")
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${project.findProperty("kotlin_version")}")

    // Create 6 软联动（未装 Create 时功能不注册；Fabric 端不涉及）
    implementation("com.simibubi.create:create-1.21.1:6.0.10-280:slim") { isTransitive = false }
    implementation("net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-1.21.1:1.0.6")
    compileOnly("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")
}

sourceSets {
    main {
        java {
            srcDir(project(":common").sourceSets["main"].java)
        }
        kotlin {
            srcDir(project(":common").sourceSets["main"].kotlin)
        }
        resources {
            srcDir(project(":common").sourceSets["main"].resources)
            exclude("architectury.common.json")
            exclude("xbce.accesswidener")
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from(project(":common").sourceSets["main"].output)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 21
}
