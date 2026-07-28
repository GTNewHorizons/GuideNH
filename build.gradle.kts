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
        // Forward the layout-overlay flag to the client JVM:
        //   ./gradlew runClient25 -Dguidenh.layoutOverlay=true
        providers.systemProperty("guidenh.layoutOverlay").orNull?.let {
            jvmArgs("-Dguidenh.layoutOverlay=$it")
        }
        providers.systemProperty("guidenh.debug.scenerender").orNull?.let {
            jvmArgs("-Dguidenh.debug.scenerender=$it")
        }
        // Forward headless-render driver props to the client JVM:
        //   ./gradlew runClient25 -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.page=guidenh:guidenh/en_us/markdown
        providers.systemProperty("guidenh.headlessRender").orNull?.let {
            jvmArgs("-Dguidenh.headlessRender=$it")
        }
        listOf("guide", "page", "md", "width", "out", "lang", "bounds", "overlay", "world", "scale", "allPages", "list").forEach { key ->
            providers.systemProperty("guidenh.renderpage.$key").orNull?.let {
                jvmArgs("-Dguidenh.renderpage.$key=$it")
            }
        }
    }
}


/** Standalone task: build Rust native library.
 *  Run manually: ./gradlew buildRustNative
 *  Does NOT wire into the main build pipeline.
 *  Requires Rust toolchain: https://rustup.rs
 */
/** Resolve the Rust DLL path eagerly (configuration-cache friendly): prefer the
 *  redirected target dir on E:, fall back to the in-tree target dir. */
val rustDllPath: String = run {
    val eDrive = File("E:/build_out/guide_nh_rust/release/guide_layout_engine.dll")
    if (eDrive.exists()) eDrive.absolutePath else "${rootDir}/layout-engine/target/release/guide_layout_engine.dll"
}

/** Run GlyphRenderTest main class. Requires Rust DLL built first. */
val runGlyphTest by tasks.registering(JavaExec::class) {
    description = "Run GlyphRenderTest visual glyph verification window"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphRenderTest")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
}

/** Headless diagnostic: print glyph pipeline to console. */
val runGlyphDiag by tasks.registering(JavaExec::class) {
    description = "Run GlyphDiag: headless glyph data diagnostic"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphDiag")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
}

/** Headless layout pipeline test bench: synthetic pages → invariants + tree dump. */
val runLayoutDump by tasks.registering(JavaExec::class) {
    description = "Run LayoutPipelineHarness: headless layout pipeline verification"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.LayoutPipelineHarness")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
    setIgnoreExitValue(true)
}

/** Headless A/B: cosmic renderText vs parley renderTextParley. */
val runParleySmoke by tasks.registering(JavaExec::class) {
    description = "Headless A/B: cosmic renderText vs parley renderTextParley"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphRenderTest")
    jvmArgs("-Dguide.native.lib.path=$rustDllPath", "-Dsun.java2d.uiScale=1.0")
    args("--headless")
}

val buildRustNative by tasks.registering(Exec::class) {
    description = "Build Rust native library (layout-engine/guide_layout_engine.dll)"
    group = "build"
    workingDir = file("layout-engine")
    commandLine("cargo", "build", "--release")
    outputs.file("layout-engine/target/release/guide_layout_engine.dll")
}
