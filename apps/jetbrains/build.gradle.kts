plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.liushoukun"
version = "1.0.1"

/** 仓库根目录（本工程位于 apps/jetbrains） */
val monorepoRoot: java.io.File = rootDir.parentFile.parentFile

/** runIde 启动时自动打开 monorepo 下的示例目录（含 sample.md / sample.infographic） */
val runIdeOpenDir: java.io.File = monorepoRoot.resolve("examples")

repositories {
    mavenCentral()
}

intellij {
    type.set(providers.gradleProperty("platformType"))
    version.set(providers.gradleProperty("platformVersion"))
    plugins.set(listOf("org.intellij.plugins.markdown"))
}

val previewWebDist = monorepoRoot.resolve("packages/preview-web/dist/preview.js")
val previewWebCss = monorepoRoot.resolve("packages/preview-web/preview.css")

val buildPreviewWeb =
    tasks.register<Exec>("buildPreviewWeb") {
        group = "build"
        description = "构建共享 Markdown 预览脚本（pnpm：@antv-infographic/preview-web）"
        workingDir = monorepoRoot
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            commandLine("cmd", "/c", "pnpm run --filter @antv-infographic/preview-web build")
        } else {
            commandLine("pnpm", "run", "--filter", "@antv-infographic/preview-web", "build")
        }
    }

val syncPreviewJs =
    tasks.register<Copy>("syncPreviewJs") {
        group = "build"
        description = "将 preview-web 的 preview.js / preview.css 复制到 src/main/resources/web/"
        dependsOn(buildPreviewWeb)
        from(previewWebDist)
        from(previewWebCss)
        into(layout.projectDirectory.dir("src/main/resources/web"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

val vscodeDistEditorWebview = monorepoRoot.resolve("apps/vscode/dist/editorWebview.js")

val buildVscodeWebviewBundle =
    tasks.register<Exec>("buildVscodeWebviewBundle") {
        group = "build"
        description = "在 apps/vscode 执行 esbuild，生成 editorWebview.js（需已构建 preview-web）"
        dependsOn(syncPreviewJs)
        workingDir = monorepoRoot.resolve("apps/vscode")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            commandLine("cmd", "/c", "node esbuild.config.mjs")
        } else {
            commandLine("node", "esbuild.config.mjs")
        }
    }

val syncEditorWebview =
    tasks.register<Copy>("syncEditorWebview") {
        group = "build"
        description = "将 apps/vscode/dist/editorWebview.js 复制到 src/main/resources/web/"
        dependsOn(buildVscodeWebviewBundle)
        from(vscodeDistEditorWebview)
        into(layout.projectDirectory.dir("src/main/resources/web"))
    }

tasks.named("processResources") {
    dependsOn(syncEditorWebview)
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    runIde {
        jvmArgs("-Xmx2048m")
        // RunIdeTask 继承 JavaExec，参数传给 com.intellij.idea.Main，与命令行打开工程一致
        args(runIdeOpenDir.absolutePath)
    }
}
