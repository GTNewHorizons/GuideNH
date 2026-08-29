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
        // Forward extra development resource-pack source (visual-test fixture pack):
        //   ./gradlew runClient25 -Dguidenh.guide.sources=D:/Projects/GuideNH/visualtest/resourcepack
        providers.systemProperty("guidenh.guide.sources").orNull?.let {
            jvmArgs("-Dguideme.resourcePack.sources=$it")
        }
        // Forward headless-render driver props to the client JVM:
        //   ./gradlew runClient25 -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.page=guidenh:guidenh/en_us/markdown
        providers.systemProperty("guidenh.headlessRender").orNull?.let {
            jvmArgs("-Dguidenh.headlessRender=$it")
        }
        listOf("guide", "page", "md", "width", "out", "lang", "bounds", "overlay", "world", "scale", "allPages", "list", "chrome", "navscroll", "mermaidzoom", "mermaidoffset", "guiscale", "title").forEach { key ->
            providers.systemProperty("guidenh.renderpage.$key").orNull?.let {
                jvmArgs("-Dguidenh.renderpage.$key=$it")
            }
        }
    }
}

/** Run GlyphRenderTest main class. */
val runGlyphTest by tasks.registering(JavaExec::class) {
    description = "Run GlyphRenderTest visual glyph verification window"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphRenderTest")
    jvmArgs("-Dsun.java2d.uiScale=1.0")
}

/** Headless diagnostic: print glyph pipeline to console. */
val runGlyphDiag by tasks.registering(JavaExec::class) {
    description = "Run GlyphDiag: headless glyph data diagnostic"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphDiag")
    jvmArgs("-Dsun.java2d.uiScale=1.0")
}

/** Headless layout pipeline test bench: synthetic pages and tree dump. */
val runLayoutDump by tasks.registering(JavaExec::class) {
    description = "Run LayoutPipelineHarness: headless layout pipeline verification"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.LayoutPipelineHarness")
    jvmArgs("-Dsun.java2d.uiScale=1.0")
    setIgnoreExitValue(true)
}

/** Headless text rendering smoke task. */
val runParleySmoke by tasks.registering(JavaExec::class) {
    description = "Headless text rendering smoke test"
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.hfstudio.guidenh.guide.layout.GlyphRenderTest")
    jvmArgs("-Dsun.java2d.uiScale=1.0")
    args("--headless")
}


