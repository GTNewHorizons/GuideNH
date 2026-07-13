import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

minecraft {
    extraRunJvmArguments.addAll("-Xmx4G", "-Xms512m", "-Dgtnhlib.dumpkeys=true")
}

dependencies {
    // fastutil is available at MC runtime but not in test scope
    testImplementation("it.unimi.dsi:fastutil:8.5.12")
}

tasks.withType<JavaCompile>().configureEach {
    options.annotationProcessorPath = configurations.annotationProcessor.get()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    val nativeLib = System.getProperty("guide.native.lib.path")
    if (nativeLib != null) {
        jvmArgs("-Dguide.native.lib.path=$nativeLib")
    }
}

tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    exclude("META-INF/maven/**", "META-INF/LICENSE*", "META-INF/NOTICE*")
    minimize {
        exclude(dependency("org.apache.lucene:lucene-core:.*"))
        exclude(dependency("org.apache.lucene:lucene-analyzers-common:.*"))
        exclude(dependency("org.apache.lucene:lucene-queryparser:.*"))
        exclude(dependency("org.apache.lucene:lucene-highlighter:.*"))
        exclude(dependency("org.scilab.forge:jlatexmath:.*"))
        exclude(dependency("org.eclipse.elk:org.eclipse.elk.core:.*"))
        exclude(dependency("org.eclipse.elk:org.eclipse.elk.alg.common:.*"))
        exclude(dependency("org.eclipse.elk:org.eclipse.elk.alg.layered:.*"))
        exclude(dependency("org.eclipse.xtext:org.eclipse.xtext.xbase.lib:.*"))
    }
}

val runConfigs = listOf(
    "runClient" to "run/client",
    "runClient17" to "run/client_new",
    "runClient21" to "run/client_new",
    "runClient25" to "run/client_new",
    "runServer" to "run/server",
    "runServer17" to "run/server_new",
    "runServer21" to "run/server_new",
    "runServer25" to "run/server_new"
)

runConfigs.forEach { (taskName, path) ->
    tasks.named<JavaExec>(taskName) {
        workingDir = file("${projectDir}/$path")
        doFirst {
            workingDir.mkdirs()
        }
    }
}

/** Standalone task: build Rust native library.
 *  Run manually: ./gradlew buildRustNative
 *  Does NOT wire into the main build pipeline.
 *  Requires Rust toolchain: https://rustup.rs
 */
val buildRustNative by tasks.registering(Exec::class) {
    description = "Build Rust native library (layout-engine/guide_layout_engine.dll)"
    group = "build"
    workingDir = file("layout-engine")
    commandLine("cargo", "build", "--release")
    outputs.file("layout-engine/target/release/guide_layout_engine.dll")
}
