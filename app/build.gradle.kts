import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.aipal.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val propVersionCode = (project.findProperty("version.code") as? String)?.toIntOrNull() ?: 2
  val propVersionName = (project.findProperty("version.name") as? String) ?: "0.2.0-alpha"

  defaultConfig {
    applicationId = "com.aipal.app"
    minSdk = 24
    targetSdk = 36
    versionCode = propVersionCode
    versionName = propVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

// Semantic Versioning Automation and Utilities
data class SemVer(val major: Int, val minor: Int, val patch: Int, val preRelease: String?) {
    override fun toString(): String {
        return if (preRelease.isNullOrBlank()) {
            "$major.$minor.$patch"
        } else {
            "$major.$minor.$patch-$preRelease"
        }
    }
    
    companion object {
        fun parse(versionStr: String): SemVer {
            val parts = versionStr.split("-", limit = 2)
            val mainParts = parts[0].split(".")
            val major = mainParts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = mainParts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = mainParts.getOrNull(2)?.toIntOrNull() ?: 0
            val preRelease = parts.getOrNull(1)
            return SemVer(major, minor, patch, preRelease)
        }
    }
}

tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the current semantic version information."
    doLast {
        val propVersionCode = project.findProperty("version.code")?.toString()?.toIntOrNull() ?: 2
        val propVersionName = project.findProperty("version.name")?.toString() ?: "0.2.0-alpha"
        println("---------------------------------------------")
        println("AI PAL Semantic Version Info:")
        println("  Version Code: $propVersionCode")
        println("  Version Name: $propVersionName")
        println("---------------------------------------------")
    }
}

tasks.register("bumpVersionCode") {
    group = "versioning"
    description = "Increments the version.code in gradle.properties."
    doLast {
        val file = file("${rootDir}/gradle.properties")
        val lines = file.readLines().toMutableList()
        var currentCode = 2
        var updated = false
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("version.code=")) {
                currentCode = lines[i].substringAfter("=").trim().toIntOrNull() ?: 2
                val newCode = currentCode + 1
                lines[i] = "version.code=$newCode"
                println("Bumping Version Code from $currentCode to $newCode")
                updated = true
                break
            }
        }
        if (!updated) {
            lines.add("version.code=3")
            println("Added version.code=3")
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.register("bumpPatch") {
    group = "versioning"
    description = "Increments the patch version in gradle.properties."
    doLast {
        val file = file("${rootDir}/gradle.properties")
        val lines = file.readLines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("version.name=")) {
                val currentName = lines[i].substringAfter("=").trim()
                val semVer = SemVer.parse(currentName)
                val newSemVer = SemVer(semVer.major, semVer.minor, semVer.patch + 1, semVer.preRelease)
                lines[i] = "version.name=$newSemVer"
                println("Bumping Patch version from $currentName to $newSemVer")
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.register("bumpMinor") {
    group = "versioning"
    description = "Increments the minor version and resets patch in gradle.properties."
    doLast {
        val file = file("${rootDir}/gradle.properties")
        val lines = file.readLines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("version.name=")) {
                val currentName = lines[i].substringAfter("=").trim()
                val semVer = SemVer.parse(currentName)
                val newSemVer = SemVer(semVer.major, semVer.minor + 1, 0, semVer.preRelease)
                lines[i] = "version.name=$newSemVer"
                println("Bumping Minor version from $currentName to $newSemVer")
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.register("bumpMajor") {
    group = "versioning"
    description = "Increments the major version and resets minor/patch in gradle.properties."
    doLast {
        val file = file("${rootDir}/gradle.properties")
        val lines = file.readLines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("version.name=")) {
                val currentName = lines[i].substringAfter("=").trim()
                val semVer = SemVer.parse(currentName)
                val newSemVer = SemVer(semVer.major + 1, 0, 0, semVer.preRelease)
                lines[i] = "version.name=$newSemVer"
                println("Bumping Major version from $currentName to $newSemVer")
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}

tasks.register("promoteToRelease") {
    group = "versioning"
    description = "Removes the pre-release tag for a stable production release in gradle.properties."
    doLast {
        val file = file("${rootDir}/gradle.properties")
        val lines = file.readLines().toMutableList()
        for (i in lines.indices) {
            if (lines[i].trim().startsWith("version.name=")) {
                val currentName = lines[i].substringAfter("=").trim()
                val semVer = SemVer.parse(currentName)
                val newSemVer = SemVer(semVer.major, semVer.minor, semVer.patch, null)
                lines[i] = "version.name=$newSemVer"
                println("Promoting version from $currentName to $newSemVer (Stable)")
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}

