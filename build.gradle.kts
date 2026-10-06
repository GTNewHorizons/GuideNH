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
    exclude("images/**")
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
        // Forward the layout-overlay flag to the client JVM:
        //   ./gradlew runClient25 -Dguidenh.layoutOverlay=true
        providers.systemProperty("guidenh.layoutOverlay").orNull?.let {
            jvmArgs("-Dguidenh.layoutOverlay=$it")
        }
        providers.systemProperty("guidenh.debug.scenerender").orNull?.let {
            jvmArgs("-Dguidenh.debug.scenerender=$it")
        }
        // Forward extra development resource-pack source (visual-test fixture pack):
        //   ./gradlew runClient25 -Dguidenh.guide.sources=<repo>/test/visual/resourcepack
        providers.systemProperty("guidenh.guide.sources").orNull?.let {
            jvmArgs("-Dguideme.resourcePack.sources=$it")
        }
        // Forward headless-render driver props to the client JVM:
        //   ./gradlew runClient25 -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.page=guidenh:guidenh/en_us/markdown
        providers.systemProperty("guidenh.headlessRender").orNull?.let {
            jvmArgs("-Dguidenh.headlessRender=$it")
        }
        listOf("guide", "page", "md", "width", "out", "lang", "bounds", "overlay", "world", "scale", "allPages", "list", "chrome", "navscroll", "mermaidzoom", "mermaidoffset", "guiscale", "route", "query", "title").forEach { key ->
            providers.systemProperty("guidenh.renderpage.$key").orNull?.let {
                jvmArgs("-Dguidenh.renderpage.$key=$it")
            }
        }
    }
}


/** Resolve the Rust DLL path eagerly (configuration-cache friendly).
 *  Resolution order for the cargo target dir:
 *   1. Gradle property `guidenh.rustTargetDir` (-Pguidenh.rustTargetDir=...);
 *   2. Environment variable GUIDENH_RUST_TARGET_DIR;
 *   3. `target-dir` declared in `src/rust/layout-engine/.cargo/config.toml`
 *      (local, untracked cargo target redirection; relative values resolve
 *      against the crate directory);
 *   4. In-tree default `src/rust/layout-engine/target`. */
val rustTargetDir: String = run {
    val fromProperty = providers.gradleProperty("guidenh.rustTargetDir").orNull
    val fromEnv = System.getenv("GUIDENH_RUST_TARGET_DIR")
    val crateDir = File("${rootDir}/src/rust/layout-engine")
    val cargoConfig = File(crateDir, ".cargo/config.toml")
    val fromCargoConfig = if (cargoConfig.isFile) {
        Regex("""(?m)^\s*target-dir\s*=\s*"([^"]+)"""")
            .find(cargoConfig.readText())
            ?.groupValues?.get(1)
            ?.let { val f = File(it); if (f.isAbsolute) f.absolutePath else File(crateDir, it).absolutePath }
    } else {
        null
    }
    (fromProperty ?: fromEnv ?: fromCargoConfig ?: File(crateDir, "target").absolutePath)
}
val rustDllPath: String = "$rustTargetDir/release/guide_layout_engine.dll"

/** Headless diagnostic: print glyph pipeline to console. */
val runGlyphDiag by tasks.registering(JavaExec::class) {
    description = "Run GlyphDiag: headless glyph data diagnostic"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphDiag")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
}

/** Headless layout pipeline test bench: synthetic pages -> invariants + tree dump. */
val runLayoutDump by tasks.registering(JavaExec::class) {
    description = "Run LayoutPipelineHarness: headless layout pipeline verification"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.LayoutPipelineHarness")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
    setIgnoreExitValue(true)
}

/** Standalone task: build the Rust native library. Run manually with
 *  `./gradlew buildRustNative`; it is not part of the main build pipeline and
 *  requires a Rust toolchain (https://rustup.rs).
 */
val buildRustNative by tasks.registering(Exec::class) {
    description = "Build Rust native library (src/rust/layout-engine, guide_layout_engine.dll)"
    group = "build"
    workingDir = file("src/rust/layout-engine")
    commandLine("cargo", "build", "--release")
    outputs.file(rustDllPath)
}

/** Copy the built Rust native library into the resources tree so it lands on the
 *  runtime classpath (`/natives/guide_layout_engine.dll`). The library is a build
 *  artifact and `src/main/resources/natives/` is not tracked. The copy is skipped
 *  (with a note) when the library has not been built, so checkouts without a Rust
 *  toolchain still build; CI never runs cargo. */
val copyRustNative by tasks.registering(Copy::class) {
    description = "Copy the built layout-engine native library into src/main/resources/natives (skipped when absent)"
    group = "build"
    val dll = File(rustDllPath)
    onlyIf {
        if (dll.isFile) {
            true
        } else {
            logger.lifecycle("copyRustNative: $dll not built, skipping (build it with ./gradlew buildRustNative)")
            false
        }
    }
    from(dll.parentFile) { include(dll.name) }
    into("src/main/resources/natives")
}

tasks.named("processResources") { dependsOn(copyRustNative) }

tasks.named<Jar>("sourcesJar") {
    // Declared dependency: sourcesJar reads src/main/resources, which copyRustNative
    // writes into. The native library is not a source and is excluded from the jar.
    dependsOn(copyRustNative)
    exclude("natives/**")
}
