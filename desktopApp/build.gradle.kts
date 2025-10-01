import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
        val jvmMain by getting  {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
                implementation("io.github.aakira:napier:2.6.1")
                
                implementation("io.ktor:ktor-client-core:2.3.6")
                implementation("io.ktor:ktor-client-cio:2.3.6")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
                
                // SLF4J 로깅 구현체
                implementation("org.slf4j:slf4j-simple:1.7.36")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        jvmArgs += listOf(
            "-Djava.awt.headless=false",
            "-Dapple.awt.enableTemplateImages=false",
            "-Dapple.laf.useScreenMenuBar=false",
            "-Dcom.apple.macos.useScreenMenuBar=false",
            "-Djava.awt.accessibility=false",
            "-Dsun.awt.noerasebackground=true",  // 윈도우 깜빡임 방지
            "-Dsun.java2d.opengl=true"           // 윈도우 OpenGL 가속
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinMultiplatformComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
