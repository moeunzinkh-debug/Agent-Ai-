plugins {
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.example.llama"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    // Android 7.0. The prebuilt llama.android AAR forced API 30 because its .so files were
    // linked against android-30 stubs; compiling llama.cpp ourselves lets us target 24.
    minSdk = 24

    ndk {
      // arm64-v8a covers essentially every Android 7+ phone still in use.
      // armeabi-v7a is deliberately excluded: llama.cpp's sgemm/NEON FP16 paths do not
      // compile cleanly for 32-bit ARM with current NDK clang.
      abiFilters += listOf("arm64-v8a", "x86_64")
    }

    externalNativeBuild {
      cmake {
        arguments += listOf(
          "-DANDROID_STL=c++_shared",
          "-DCMAKE_BUILD_TYPE=Release"
        )
        cppFlags += "-O3"
      }
    }

    consumerProguardFiles("consumer-rules.pro")
  }

  externalNativeBuild {
    cmake {
      path("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { jvmToolchain(17) }

  packaging {
    jniLibs {
      // llama.cpp resolves its own shared libs at runtime; keep them extracted on disk.
      useLegacyPackaging = true
    }
  }
}

dependencies {
  implementation(libs.kotlinx.coroutines.android)
}
