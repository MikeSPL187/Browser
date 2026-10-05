import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateLauncherShortcutResources : DefaultTask() {
    @get:Input
    abstract val applicationId: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val xmlDirectory = outputDirectory.dir("xml").get().asFile
        xmlDirectory.mkdirs()
        xmlDirectory.resolve("shortcuts.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="launcher_new_tab"
        android:enabled="true"
        android:icon="@mipmap/ic_shortcut_new_tab"
        android:shortcutShortLabel="@string/new_tab_title"
        android:shortcutLongLabel="@string/command_new_regular_tab_name">
        <intent
            android:action="dev.sk2andy.materialbrowser.action.NEW_TAB"
            android:targetPackage="${applicationId.get()}"
            android:targetClass="dev.sk2andy.materialbrowser.LauncherShortcutActivity" />
    </shortcut>
    <shortcut
        android:shortcutId="launcher_new_private_tab"
        android:enabled="true"
        android:icon="@mipmap/ic_shortcut_private_tab"
        android:shortcutShortLabel="@string/incognito"
        android:shortcutLongLabel="@string/command_new_incognito_tab_name">
        <intent
            android:action="dev.sk2andy.materialbrowser.action.NEW_PRIVATE_TAB"
            android:targetPackage="${applicationId.get()}"
            android:targetClass="dev.sk2andy.materialbrowser.LauncherShortcutActivity" />
    </shortcut>
</shortcuts>
""".trimIndent(),
            Charsets.UTF_8,
        )
    }
}

abstract class GenerateGeckoContentTopInsetScript : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val source = sourceFile.get().asFile.readText(Charsets.UTF_8)
        val template = source
            .substringAfter("        \"\"\"\n", missingDelimiterValue = "")
            .substringBefore("\n        \"\"\".trimIndent()", missingDelimiterValue = "")
        check(template.isNotEmpty()) { "WebContentTopInsetScript template was not found." }
        val escapedDollar = "${'$'}" + "{'${'$'}'}"
        val script = template
            .trimIndent()
            .replace(escapedDollar, "${'$'}")
            .replace("${'$'}bridgeName", "CandyContentTopInset")
        check(escapedDollar !in script) {
            "WebContentTopInsetScript contains unresolved Kotlin dollar escapes."
        }
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(script + "\n", Charsets.UTF_8)
    }
}

abstract class GenerateSystemWebViewThirdPartyNotices : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val source = sourceFile.get().asFile.readText(Charsets.UTF_8)
        val geckoSectionStart = "\nGecko default extensions\n------------------------\n"
        val apacheLicenseStart = "\n\n                                 Apache License"
        val startIndex = source.indexOf(geckoSectionStart)
        val endIndex = source.indexOf(apacheLicenseStart, startIndex + geckoSectionStart.length)
        check(startIndex >= 0 && endIndex > startIndex) {
            "Gecko extension notice section boundaries were not found."
        }
        val generated = source
            .removeRange(startIndex, endIndex)
            .replace(
                "This inventory reflects the GeckoView release runtime classpath for Vola.",
                "This inventory reflects the System WebView release runtime classpath for Vola.",
            )
        check("Gecko default extensions" !in generated && ".xpi" !in generated.lowercase()) {
            "System WebView notices still describe Gecko extension packages."
        }
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(generated, Charsets.UTF_8)
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}

fun releaseSigningValue(propertyName: String, environmentVariable: String): String? =
    releaseKeystoreProperties.getProperty(propertyName)
        ?.takeIf(String::isNotBlank)
        ?: providers.environmentVariable(environmentVariable).orNull?.takeIf(String::isNotBlank)

fun validatedApplicationIdSuffix(value: String): String {
    require(value.matches(Regex("""\.[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)*"""))) {
        "vola.localReleaseApplicationIdSuffix must start with '.' and contain valid ID segments."
    }
    return value
}

fun validatedAppLabel(value: String): String {
    require(value.isNotBlank() && value.none(Char::isISOControl)) {
        "vola.localReleaseAppLabel must contain visible text without control characters."
    }
    return value
}

val releaseSigningValues = mapOf(
    "storeFile" to releaseSigningValue("storeFile", "VOLA_RELEASE_KEYSTORE_PATH"),
    "storePassword" to releaseSigningValue("storePassword", "VOLA_RELEASE_STORE_PASSWORD"),
    "keyAlias" to releaseSigningValue("keyAlias", "VOLA_RELEASE_KEY_ALIAS"),
    "keyPassword" to releaseSigningValue("keyPassword", "VOLA_RELEASE_KEY_PASSWORD"),
)
val missingReleaseSigningValues = releaseSigningValues.filterValues { it == null }.keys
val hasReleaseSigning = missingReleaseSigningValues.isEmpty()
val volaVersionCode = providers.gradleProperty("vola.versionCode").orElse("1")
val volaVersionName = providers.gradleProperty("vola.versionName").orElse("0.1")
val volaReleaseNotesFile = providers.gradleProperty("vola.releaseNotesFile")
    .orElse(volaVersionName.map { version -> "release-notes/$version.md" })
val releaseNotesImageSyntax = Regex("""!\[([^]]+)]\(([^)]+)\)""")
val releaseNotesImageFiles = providers.provider {
    val notes = rootProject.file(volaReleaseNotesFile.get())
    if (!notes.isFile) {
        emptyList()
    } else {
        val prefix = "https://raw.githubusercontent.com/MikeSPL187/Browser/" +
            "v${volaVersionName.get()}/docs/screenshots/"
        releaseNotesImageSyntax.findAll(notes.readText(Charsets.UTF_8))
            .mapNotNull { match ->
                match.groupValues[2]
                    .takeIf { target -> target.startsWith(prefix) }
                    ?.removePrefix(prefix)
                    ?.let { fileName -> rootProject.file("docs/screenshots/$fileName") }
            }
            .toList()
    }
}
val debugApplicationIdSuffix =
    providers.gradleProperty("vola.debugApplicationIdSuffix").orElse(".debug")
val debugAppLabel = providers.gradleProperty("vola.debugAppLabel").orElse("Vola Debug")
val localReleaseApplicationIdSuffix =
    providers.gradleProperty("vola.localReleaseApplicationIdSuffix")
        .orElse(".preview")
        .map(::validatedApplicationIdSuffix)
val localReleaseAppLabel =
    providers.gradleProperty("vola.localReleaseAppLabel")
        .orElse("Vola Preview")
        .map(::validatedAppLabel)
val releaseAbi = providers.gradleProperty("vola.releaseAbi").map { value ->
    require(value == "arm64-v8a") {
        "vola.releaseAbi must be arm64-v8a."
    }
    value
}
val compressNativeLibs = providers.gradleProperty("vola.compressNativeLibs")
    .map(String::toBooleanStrict)
    .orElse(false)
val performanceDiagnostics = providers.gradleProperty("vola.performanceDiagnostics")
    .map(String::toBooleanStrict)
    .orElse(false)

android {
    namespace = "dev.sk2andy.materialbrowser"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "io.github.mikespl187.vola"
        minSdk = 31
        targetSdk = 36
        versionCode = volaVersionCode.get().toInt()
        versionName = volaVersionName.get()
        manifestPlaceholders["appLabel"] = "@string/app_name"
        manifestPlaceholders["networkSecurityConfig"] = "@xml/network_security_config"
        buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "false")
        buildConfigField("boolean", "SYSTEM_WEBVIEW_ONLY", "false")
        buildConfigField("boolean", "TRUST_USER_CERTIFICATES", "false")
        buildConfigField(
            "boolean",
            "ENABLE_PERFORMANCE_DIAGNOSTICS",
            performanceDiagnostics.get().toString(),
        )
        manifestPlaceholders["performanceDiagnosticsEnabled"] =
            performanceDiagnostics.get().toString()
        buildConfigField("String", "RELEASE_NOTES_VERSION", "\"${volaVersionName.get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        releaseAbi.orNull?.let { abi ->
            ndk {
                abiFilters += abi
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            proguardFile("proguard-gecko-rules.pro")
        }

        create("systemwebview") {
            dimension = "distribution"
            applicationIdSuffix = ".systemwebview"
            manifestPlaceholders["appLabel"] = "Vola WebView"
            manifestPlaceholders["performanceDiagnosticsEnabled"] = "false"
            buildConfigField("boolean", "SYSTEM_WEBVIEW_ONLY", "true")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseSigningValues["storeFile"]))
                storePassword = requireNotNull(releaseSigningValues["storePassword"])
                keyAlias = requireNotNull(releaseSigningValues["keyAlias"])
                keyPassword = requireNotNull(releaseSigningValues["keyPassword"])
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = debugApplicationIdSuffix.get()
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = debugAppLabel.get()
        }

        release {
            isMinifyEnabled = true
            buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "true")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        // Optimized, installable preview of a release build. CI signs it with the stable preview
        // key restored as the debug keystore, so each preview updates the previous install.
        create("localRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = localReleaseApplicationIdSuffix.get()
            versionNameSuffix = "-preview"
            manifestPlaceholders["appLabel"] = localReleaseAppLabel.get()
            buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "false")
            matchingFallbacks += listOf("release")
        }

        // Release code without R8 renaming, debug-signed and profileable by the shell: the baseline
        // profile generator and the startup benchmark (:baselineprofile) run against it. Release
        // builds map src/main/baseline-prof.txt through R8 themselves.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = "-benchmark"
            manifestPlaceholders["appLabel"] = "Vola Benchmark"
            manifestPlaceholders["performanceDiagnosticsEnabled"] = "true"
            buildConfigField("boolean", "ENABLE_GITHUB_UPDATES", "false")
            matchingFallbacks += listOf("release")
        }

        create("userCaDebug") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".ca.debug"
            manifestPlaceholders["appLabel"] = "Vola CA Debug"
            manifestPlaceholders["networkSecurityConfig"] =
                "@xml/network_security_config_user_ca"
            buildConfigField("boolean", "TRUST_USER_CERTIFICATES", "true")
            matchingFallbacks += listOf("debug")
        }

        create("userCaRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".ca"
            manifestPlaceholders["appLabel"] = "Vola CA"
            manifestPlaceholders["networkSecurityConfig"] =
                "@xml/network_security_config_user_ca"
            buildConfigField("boolean", "TRUST_USER_CERTIFICATES", "true")
            matchingFallbacks += listOf("release")
        }
    }

    sourceSets {
        getByName("main").assets.srcDir(
            layout.buildDirectory.dir("generated/candySyncIcons/assets").get().asFile,
        )
        getByName("main").assets.srcDir(
            layout.buildDirectory.dir("generated/releaseNotes/assets").get().asFile,
        )
        getByName("full").assets.srcDir(
            layout.buildDirectory.dir("generated/geckoPrivacy/assets").get().asFile,
        )
        getByName("full").assets.srcDir("src/gecko/assets")
        getByName("systemwebview").assets.srcDir(
            layout.buildDirectory.dir("generated/systemWebViewNotices/assets").get().asFile,
        )
        getByName("userCaDebug").res.srcDir("src/userCa/res")
        getByName("userCaRelease").res.srcDir("src/userCa/res")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = compressNativeLibs.get()
    }
}

androidComponents {
    onVariants { variant ->
        val capitalizedVariantName = variant.name.replaceFirstChar(Char::uppercaseChar)
        val generateLauncherShortcuts = tasks.register<GenerateLauncherShortcutResources>(
            "generate${capitalizedVariantName}LauncherShortcutResources",
        ) {
            applicationId.set(variant.applicationId)
        }
        variant.sources.res?.addGeneratedSourceDirectory(
            generateLauncherShortcuts,
            GenerateLauncherShortcutResources::outputDirectory,
        )
    }
}

val generateCandySyncDeviceIconAsset by tasks.registering(Copy::class) {
    val catalog = rootProject.file("sync/protocol/device-icons-v1.json")
    inputs.file(catalog)
    from(catalog)
    into(layout.buildDirectory.dir("generated/candySyncIcons/assets"))
    rename { "candy_sync_device_icons_v1.json" }
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

val generateSystemWebViewThirdPartyNotices by tasks.registering(
    GenerateSystemWebViewThirdPartyNotices::class,
) {
    sourceFile.set(layout.projectDirectory.file("src/main/assets/third_party_notices.txt"))
    outputFile.set(
        layout.buildDirectory.file(
            "generated/systemWebViewNotices/assets/third_party_notices.txt",
        ),
    )
}

tasks.matching { task ->
    task.name.endsWith("Build") && task.name.startsWith("preSystemwebview")
}.configureEach {
    dependsOn(generateSystemWebViewThirdPartyNotices)
}

val generateGeckoPrivacyRuleAssets by tasks.registering(Sync::class) {
    val ruleAssets = listOf("candy_default_rules.txt")
    from(ruleAssets.map { fileName -> layout.projectDirectory.file("src/main/assets/$fileName") })
    into(layout.buildDirectory.dir("generated/geckoPrivacy/assets/candy_privacy/rules"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

val generateGeckoContentTopInsetScript by tasks.registering(
    GenerateGeckoContentTopInsetScript::class,
) {
    sourceFile.set(
        layout.projectDirectory.file(
            "src/main/java/dev/sk2andy/materialbrowser/browser/WebContentTopInsetScript.kt",
        ),
    )
    outputFile.set(
        layout.buildDirectory.file(
            "generated/geckoPrivacy/assets/candy_privacy/content_top_inset.js",
        ),
    )
}

val verifyGeckoDefaultExtensionAssets by tasks.registering(Exec::class) {
    group = "verification"
    description = "Verifies pinned Gecko default-extension identities and XPI integrity."
    workingDir(rootProject.projectDir)
    commandLine(
        "python3",
        rootProject.file("scripts/generate_gecko_default_extensions.py"),
        "verify",
    )
    inputs.file(
        layout.projectDirectory.file("src/gecko/assets/gecko_default_extensions/catalog.json"),
    )
    inputs.files(
        fileTree(layout.projectDirectory.dir("src/gecko/assets/gecko_default_extensions")) {
            include("*.xpi")
        },
    )
}

tasks.matching { task ->
    task.name.endsWith("Build") &&
        task.name.startsWith("preFull")
}.configureEach {
    dependsOn(generateGeckoPrivacyRuleAssets)
    dependsOn(generateGeckoContentTopInsetScript)
    dependsOn(verifyGeckoDefaultExtensionAssets)
}

val validateReleaseNotes by tasks.registering {
    group = "verification"
    description = "Validates the Markdown release notes packaged into the app."
    val releaseNotes = volaReleaseNotesFile.map(rootProject::file)
    inputs.property("releaseNotesVersion", volaVersionName)
    inputs.property("releaseNotesPath", volaReleaseNotesFile)
    inputs.file(releaseNotes)

    doLast {
        val version = volaVersionName.get()
        val expectedPath = "release-notes/$version.md"
        val configuredPath = volaReleaseNotesFile.get().replace('\\', '/')
        check(configuredPath == expectedPath) {
            "Release notes must use the version-matched path $expectedPath, got $configuredPath."
        }
        val file = releaseNotes.get()
        check(file.isFile) { "Release notes do not exist: ${file.absolutePath}" }
        val bytes = file.readBytes()
        check(bytes.isNotEmpty() && bytes.size <= 65_536) {
            "Release notes must contain 1..65536 UTF-8 bytes, got ${bytes.size}."
        }
        val content = bytes.toString(Charsets.UTF_8)
        check(!content.startsWith('\uFEFF') && content.lineSequence().firstOrNull()?.startsWith("# ") == true) {
            "Release notes must be UTF-8 without BOM and start with one '# ' heading."
        }
        val lines = content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
        check(lines.all { line -> line.length <= 8_192 }) {
            "Release notes contain a line longer than 8192 characters."
        }
        check(lines.count(String::isNotBlank) <= 256) {
            "Release notes must contain at most 256 non-empty lines."
        }
        var codeBlockOpen = false
        lines.forEach { line ->
            if (!codeBlockOpen && line.startsWith("```")) {
                codeBlockOpen = true
            } else if (codeBlockOpen && line == "```") {
                codeBlockOpen = false
            }
        }
        check(!codeBlockOpen) { "Release notes contain an unclosed fenced code block." }
        check(content.none { character ->
            character.code in 0..31 && character != '\n' && character != '\r' && character != '\t'
        }) {
            "Release notes contain unsupported control characters."
        }
        val imageMatches = releaseNotesImageSyntax.findAll(content).toList()
        check(imageMatches.size <= 2) { "Release notes support at most two screenshots." }
        val expectedImagePrefix = "https://raw.githubusercontent.com/MikeSPL187/Browser/" +
            "v$version/docs/screenshots/"
        val imageFiles = imageMatches.map { match ->
            val altText = match.groupValues[1].trim()
            val target = match.groupValues[2]
            check(altText.isNotEmpty()) { "Every release-note screenshot needs useful alt text." }
            check(target.startsWith(expectedImagePrefix)) {
                "Release-note screenshots must use tag-pinned URLs below $expectedImagePrefix."
            }
            val fileName = target.removePrefix(expectedImagePrefix)
            check(fileName.matches(Regex("""[A-Za-z0-9][A-Za-z0-9._-]*\.(png|jpe?g|webp)"""))) {
                "Unsupported release-note screenshot path: $fileName."
            }
            rootProject.file("docs/screenshots/$fileName").also { image ->
                check(image.isFile) { "Release-note screenshot does not exist: ${image.absolutePath}" }
                check(image.length() in 1..2_097_152) {
                    "Release-note screenshot must contain 1..2097152 bytes: ${image.absolutePath}."
                }
            }
        }
        check(imageFiles.sumOf { image -> image.length() } <= 4_194_304) {
            "Release-note screenshots exceed the 4 MiB packaged limit."
        }
    }
}

val generateReleaseNotesAsset by tasks.registering(Sync::class) {
    dependsOn(validateReleaseNotes)
    inputs.files(releaseNotesImageFiles)
    from(volaReleaseNotesFile.map(rootProject::file)) {
        rename { "candy_release_notes.md" }
    }
    from(releaseNotesImageFiles) {
        into("release-notes-images")
    }
    into(layout.buildDirectory.dir("generated/releaseNotes/assets"))
    duplicatesStrategy = DuplicatesStrategy.FAIL
}

tasks.named("preBuild").configure {
    dependsOn(generateCandySyncDeviceIconAsset)
    dependsOn(generateReleaseNotesAsset)
}

val validateReleaseSigning by tasks.registering {
    group = "verification"
    description = "Checks that release signing credentials and the keystore are available."

    doLast {
        check(missingReleaseSigningValues.isEmpty()) {
            "Missing release signing values: ${missingReleaseSigningValues.sorted().joinToString()}. " +
                "Configure keystore.properties or the VOLA_RELEASE_* environment variables."
        }

        val keystoreFile = rootProject.file(requireNotNull(releaseSigningValues["storeFile"]))
        check(keystoreFile.isFile) {
            "Release keystore does not exist: ${keystoreFile.absolutePath}"
        }
    }
}

tasks.matching {
    it.name == "preFullReleaseBuild" ||
        it.name == "preFullUserCaReleaseBuild" ||
        it.name == "preSystemwebviewReleaseBuild"
}.configureEach {
    dependsOn(validateReleaseSigning)
}

val verifySystemWebViewReleaseDependencies by tasks.registering {
    group = "verification"
    description = "Rejects GeckoView from the System WebView-only release runtime."

    doLast {
        val violations = configurations.getByName("systemwebviewReleaseRuntimeClasspath")
            .incoming
            .resolutionResult
            .allComponents
            .mapNotNull { it.moduleVersion }
            .filter { module -> module.group == "org.mozilla.geckoview" }
            .map { it.toString() }
            .sorted()

        check(violations.isEmpty()) {
            "System WebView release contains GeckoView runtime dependencies: " +
                violations.joinToString()
        }
    }
}

tasks.matching { it.name == "preSystemwebviewReleaseBuild" }.configureEach {
    dependsOn(verifySystemWebViewReleaseDependencies)
}

val verifyNoPlayServicesDependencies by tasks.registering {
    group = "verification"
    description = "Rejects proprietary Google runtime dependencies from every release APK."

    doLast {
        val forbiddenGroups = listOf(
            "com.google.android.datatransport",
            "com.google.android.gms",
            "com.google.android.odml",
            "com.google.firebase",
            "com.google.mlkit",
        )
        val violations = listOf(
            "fullReleaseRuntimeClasspath",
            "systemwebviewReleaseRuntimeClasspath",
        ).flatMap { configurationName ->
            configurations.getByName(configurationName)
                .incoming
                .resolutionResult
                .allComponents
                .mapNotNull { it.moduleVersion }
                .filter { module ->
                    forbiddenGroups.any { group ->
                        module.group == group || module.group.startsWith("$group.")
                    }
                }
                .map { "$configurationName: $it" }
        }.sorted()

        check(violations.isEmpty()) {
            "Release contains forbidden Google runtime dependencies: " + violations.joinToString()
        }
    }
}

tasks.matching { task ->
    task.name == "preFullReleaseBuild" || task.name == "preSystemwebviewReleaseBuild"
}.configureEach {
    dependsOn(verifyNoPlayServicesDependencies)
}

dependencies {
    implementation(project(":shared"))
    // Installs the bundled baseline profile on devices without Play, such as GitHub installs.
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    // Gecko uses Android's framework Credential Manager for WebAuthn on API 34+. AndroidX keeps
    // password save/select available through the system and installed credential providers.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.guava)
    implementation(libs.blurview)
    implementation(libs.okhttp)
    implementation(libs.argon2kt)
    implementation(libs.kotlinx.serialization.json)
    // Vola ships without Google Play services. GeckoView uses the framework Credential Manager for
    // WebAuthn on API 34+, so its optional Play services FIDO provider is excluded.
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(
        "fullImplementation",
        libs.geckoview,
    ) {
        exclude(group = "com.google.android.gms", module = "play-services-fido")
    }
    addProvider("systemwebviewCompileOnly", libs.geckoview)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    testImplementation(libs.geckoview)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.geckoview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    addProvider("userCaDebugImplementation", libs.androidx.compose.ui.tooling.asProvider())
    addProvider("userCaDebugImplementation", libs.androidx.compose.ui.test.manifest)
}
