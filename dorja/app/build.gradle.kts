import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// ─────────────────────────────────────────────────────────────────────────────
// OpenCV Android SDK (native stitcher dependency)
//────────────────────────────────────────────────────────────────────────────
// The Maven org.opencv:opencv AAR does NOT contain the stitching module
// (verified against the artifact: zero cv::Stitcher symbols in its native
// lib). The official Android SDK does. It is downloaded once into the
// third_party/ dir (git-ignored) and statically linked — only the modules
// cv::Stitcher references end up in the APK, and per-ABI APK splits keep
// installs small (no monolithic universal build).
val openCvSdkVersion = "4.13.0"
val openCvThirdParty = rootProject.layout.projectDirectory.dir("third_party")
val openCvZip = openCvThirdParty.file("opencv-$openCvSdkVersion-android-sdk.zip")
val openCvSdkDir = openCvThirdParty.dir("opencv-$openCvSdkVersion-android-sdk/OpenCV-android-sdk")
val openCvJniDir = openCvSdkDir.dir("sdk/native/jni")

tasks.register("downloadOpenCvSdk") {
  outputs.upToDateWhen { openCvJniDir.asFile.resolve("OpenCVConfig.cmake").exists() }
  doLast {
    val zipFile = openCvZip.asFile
    val minZipSize = 300_000_000L // official 4.13.0 zip is ~318MB
    if (!zipFile.exists() || zipFile.length() < minZipSize) {
      val sourceUrl = URL(
        "https://github.com/opencv/opencv/releases/download/$openCvSdkVersion/opencv-$openCvSdkVersion-android-sdk.zip"
      )
      val tmp = File(zipFile.absolutePath + ".tmp")
      var attempt = 0
      var lastError: Exception? = null
      while (attempt < 3 && !zipFile.exists()) {
        try {
          tmp.outputStream().use { out ->
            (sourceUrl.openConnection() as HttpURLConnection).apply {
              connectTimeout = 30_000
              readTimeout = 180_000
              instanceFollowRedirects = true
              setRequestProperty("User-Agent", "dorja-gradle-opencv-downloader")
              setRequestProperty("Accept", "application/octet-stream")
            }.getInputStream().use { it.copyTo(out, 1 shl 20) }
          }
          if (tmp.length() >= minZipSize) {
            tmp.renameTo(zipFile)
          } else {
            tmp.delete()
            throw IllegalStateException(
              "OpenCV SDK zip too small (${tmp.length()} bytes) — download truncated"
            )
          }
        } catch (e: Exception) {
          // Surface the real cause in the build output — ci-errors.txt only
          // carries filtered tails, so a silent retry storm hides everything.
          println("OpenCV SDK download attempt ${attempt + 1} failed: ${e.javaClass.simpleName}: ${e.message}")
          lastError = e
          attempt++
          if (attempt < 3) Thread.sleep(2000L * attempt)
        }
      }
      if (!zipFile.exists()) {
        throw lastError ?: IllegalStateException("Failed to download OpenCV SDK")
      }
    }
    if (!openCvSdkDir.asFile.resolve("sdk/native/jni/OpenCVConfig.cmake").exists()) {
      ZipFile(zipFile).use { zf ->
        val entries = zf.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          val out = openCvThirdParty.asFile.resolve(entry.name)
          if (entry.isDirectory) {
            out.mkdirs()
            continue
          }
          if (!out.canonicalPath.startsWith(openCvThirdParty.asFile.canonicalPath)) {
            continue // zip-slip guard
          }
          out.parentFile?.mkdirs()
          zf.getInputStream(entry).use { src -> out.outputStream().use { src.copyTo(it, 1 shl 20) } }
        }
      }
    }
    check(openCvJniDir.asFile.resolve("OpenCVConfig.cmake").exists()) { "OpenCV SDK unzip failed" }
  }
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.dorja.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    externalNativeBuild {
      cmake {
        arguments += listOf("-DOPENCV_SDK_DIR=${openCvJniDir.asFile.absolutePath.replace("\\", "/")}")
      }
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
    }
  }

  splits {
    abi {
      // APK size: ship one small APK per ABI instead of one fat universal.
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = false
    }
  }

  signingConfigs {
    create("release") {
      // CI overrides via -P flags; local uses env vars
      storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore")
      storePassword = System.getenv("STORE_PASSWORD") ?: ""
      keyAlias = System.getenv("KEY_ALIAS") ?: "dorja"
      keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
    getByName("debug") {
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
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  // NOTE: the AI sources are parked outside the source tree at app/src/parkedAI/
  // (they import cactus, which is commented out). Restore guide: AI_FEATURES_RESTORE.md.
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
    implementation("com.google.code.gson:gson:2.10.1")
  // AI REMOVED FOR NOW — restore by uncommenting (see AI_FEATURES_RESTORE.md at repo root).
  // implementation("com.cactuscompute:cactus:1.4.1-beta")
  // iOS-style Cupertino widgets (buttons, dialogs, switches, pickers).
  // schott12521 fork is the maintained lineage of alexzhirkevich/compose-cupertino
  // and is compiled against Compose Multiplatform 1.7.3 — same 1.7 line as our
  // BOM 2024.09.00, so it neither bumps nor clashes with androidx Compose.
  implementation("io.github.schott12521:cupertino:2.1.0")
  // NOTE: OpenCV was removed — its AAR bundles ~100MB of native .so libs for
  // every ABI, which ballooned the APK past 150MB. Stitching is done by the
  // gyro-based equirectangular pipeline in RoomScannerScreen.kt instead.
  implementation(libs.androidx.camera.view)
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
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.zxing.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // TensorFlow/LiteRT model runtime removed — Dorja AI is instant rule-based,
  // keeping the app light and lag-free (no model loading, no NPU/GPU warm-up).

  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
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
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(kotlin("test"))
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
}// Native build must not configure before the OpenCV SDK is in place.
// Covers both `configureCMakeDebug[abi]` and `externalNativeBuild*` tasks.
afterEvaluate {
  tasks.matching {
    it.name.startsWith("externalNativeBuild") || it.name.contains("CMake")
  }.configureEach {
    dependsOn("downloadOpenCvSdk")
  }
}
// Build cache invalidation: 10/18/2026 14:48:10
