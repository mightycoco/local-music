plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

val appIconSource = layout.projectDirectory.file("src/main/icon/app_icon.svg")
val generatedIconResources = layout.buildDirectory.dir("generated/res/appIcon")

val generateAppIcon by tasks.registering {
    inputs.file(appIconSource)
    outputs.dir(generatedIconResources)

    doLast {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val svgDocument = documentBuilderFactory.newDocumentBuilder().parse(appIconSource.asFile)
        val svg = svgDocument.documentElement
        val viewBox = svg.getAttribute("viewBox").trim().split(Regex("\\s+"))
        require(viewBox.size == 4) { "App icon SVG must declare a four-value viewBox." }
        val viewportWidth = viewBox[2]
        val viewportHeight = viewBox[3]
        val paths = svg.getElementsByTagName("path")
        require(paths.length > 0) { "App icon SVG must contain at least one path." }
        val outerGroup = svg.getElementsByTagName("g").item(0) as? org.w3c.dom.Element
        val outerGroupTransform = outerGroup?.getAttribute("transform").orEmpty()
        val transformMatch =
            Regex("translate\\(([-.\\d]+)[,\\s]+([-.\\d]+)\\)\\s*scale\\(([-.\\d]+)\\)")
                .matchEntire(outerGroupTransform)
        val vectorGroupOpen =
            transformMatch?.let { match ->
                "    <group android:translateX=\"${match.groupValues[1]}\" android:translateY=\"${match.groupValues[2]}\" android:scaleX=\"${match.groupValues[3]}\" android:scaleY=\"${match.groupValues[3]}\">"
            }.orEmpty()
        val vectorGroupClose = if (transformMatch != null) "\n    </group>" else ""

        val vectorPaths = buildString {
            if (vectorGroupOpen.isNotEmpty()) appendLine(vectorGroupOpen)
            repeat(paths.length) { index ->
                val path = paths.item(index) as org.w3c.dom.Element
                val fillColor = path.getAttribute("fill").ifBlank { "#FFFFFFFF" }
                appendLine("        <path android:fillColor=\"$fillColor\" android:pathData=\"${path.getAttribute("d")}\" />")
            }
            append(vectorGroupClose)
        }.trimEnd()
        val resourcesRoot = generatedIconResources.get().asFile
        val foregroundFile = File(resourcesRoot, "drawable/ic_launcher_foreground.xml")
        val iconFile = File(resourcesRoot, "mipmap-anydpi-v26/ic_launcher.xml")
        val roundIconFile = File(resourcesRoot, "mipmap-anydpi-v26/ic_launcher_round.xml")
        val colorsFile = File(resourcesRoot, "values/app_icon_colors.xml")

        foregroundFile.parentFile.mkdirs()
        foregroundFile.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="$viewportWidth"
    android:viewportHeight="$viewportHeight">
$vectorPaths
</vector>
"""
        )
        iconFile.parentFile.mkdirs()
        val adaptiveIcon =
            """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
        iconFile.writeText(adaptiveIcon)
        roundIconFile.writeText(adaptiveIcon)
        colorsFile.parentFile.mkdirs()
        colorsFile.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#00000000</color>
</resources>
"""
        )
    }
}

val releaseStoreFile = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.localmusic.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.localmusic.player"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets.named("main") {
        res.srcDir(generatedIconResources)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateAppIcon)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
