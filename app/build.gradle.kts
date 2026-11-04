/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.compose.compiler)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.ksp)
	alias(libs.plugins.androidx.room3)
}

configurations.all {
	// remove material 2
	exclude(group = "org.jetbrains.compose.material", module = "material")
	exclude(group = "androidx.compose.material", module = "material")
	// cache SNAPSHOT dependencies for less time, default 24h
	resolutionStrategy.cacheChangingModulesFor(1, "hours")
}

kotlin {
	jvmToolchain(21)
	compilerOptions {
		freeCompilerArgs.addAll(
			"-Xskip-metadata-version-check",
			"-opt-in=kotlin.time.ExperimentalTime",
			"-opt-in=kotlin.RequiresOptIn",
			"-XXLanguage:+ExplicitBackingFields"
		)
	}
}

val isTaskRelease =
	gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

android {
	namespace = "paige.navic"
	compileSdk = libs.versions.android.compileSdk.get().toInt()
	compileSdkMinor = libs.versions.android.compileSdkMinor.get().toInt()

	buildFeatures {
		resValues = true
		buildConfig = true
	}

	defaultConfig {
		applicationId = "paige.navic"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = 61
		versionName = "v1.0.0-beta02"
		multiDexEnabled = true

		ndk {
			abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
			if (!isTaskRelease) {
				abiFilters.addAll(listOf("x86_64", "x86"))
			}
		}
	}

	signingConfigs {
		create("debugConfig") {
			storeFile = file("${rootDir}/debug.keystore")
			storePassword = "android"
			keyAlias = "androiddebugkey"
			keyPassword = "android"
		}
		create("release") {
			keyAlias = System.getenv("SIGNING_KEY_ALIAS")
			keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
			storeFile = System.getenv("SIGNING_STORE_FILE")?.let(::File)
			storePassword = System.getenv("SIGNING_STORE_PASSWORD")
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isProfileable = false
			isJniDebuggable = false
			isShrinkResources = true
			signingConfig = signingConfigs.findByName("release")?.takeIf { it.storeFile != null }
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}

		debug {
			applicationIdSuffix = ".debug"
			resValue("string", "app_name", "Yabslushal (Dev)")
			signingConfig = signingConfigs.getByName("debugConfig")
			buildConfigField("boolean", "FDROID", "false")
		}
	}

	defaultConfig {
		fun String?.wrapString(): String = this?.let {
			val escaped = it
				.removeSuffix("\n")
				.replace("\\", "\\\\")
				.replace("\n", "\\n")
				.replace("\"", "\\\"")
			"\"$escaped\""
		} ?: "null"

		val commitHash = "dev"
		val commitMessage = "dev"
		val commitAuthor = "dev"
		val commitBranchName = "main"

		buildConfigField("String", "GIT_COMMIT_HASH", commitHash.wrapString())
		buildConfigField("String", "GIT_COMMIT_MESSAGE", commitMessage.wrapString())
		buildConfigField("String", "GIT_COMMIT_AUTHOR", commitAuthor.wrapString())
		buildConfigField("String", "GIT_BRANCH_NAME", commitBranchName.wrapString())
	}

	packaging {
		resources {
			excludes += "/okhttp3/**"
			excludes += "/*.properties"
			excludes += "/org/antlr/**"
			excludes += "/com/android/tools/smali/**"
			excludes += "/org/eclipse/jgit/**"
			excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
			excludes += "/org/bouncycastle/**"
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}

		jniLibs {
			keepDebugSymbols.add("**/*.so")
		}
	}

	compileOptions {
		isCoreLibraryDesugaringEnabled = true

		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}

	dependenciesInfo {
		includeInApk = false
		includeInBundle = false
	}

	testOptions {
		unitTests {
			// android.util.Log (used by paige.navic.util.Logger) is a stub on the JVM;
			// return default values instead of throwing "not mocked".
			isReturnDefaultValues = true
		}
	}
}

room3 {
	schemaDirectory("$projectDir/schemas")
}

dependencies {
	// Compose
	implementation(libs.compose.runtime)
	implementation(libs.compose.foundation)
	implementation(libs.compose.ui)
	implementation(libs.compose.material3)
	implementation(libs.compose.material3.adaptive.nav)
	implementation(libs.compose.material3.windowSize)

	// Androidx
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.lifecycle.viewmodel)
	implementation(libs.androidx.lifecycle.runtime)
	implementation(libs.androidx.datastore.preferences)
	implementation(libs.androidx.animation.graphics)
	implementation(libs.androidx.sqlite.bundled)
	implementation(libs.androidx.room3.runtime)
	implementation(libs.androidx.navigation3.ui)
	implementation(libs.androidx.media3.exoplayer)
	implementation(libs.androidx.media3.session)
	implementation(libs.androidx.media3.datasource.okhttp)
	implementation(libs.androidx.annotation)
	implementation(libs.androidx.recyclerview)
	implementation(libs.androidx.glance.appwidget)
	implementation(libs.androidx.glance.material3)
	implementation(libs.androidx.browser)
	implementation(libs.androidx.work.runtime.ktx)

	// Kotlinx
	implementation(libs.kotlinx.datetime)
	implementation(libs.kotlinx.serialization.json)
	implementation(libs.kotlinx.collections.immutable)

	// Networking
	implementation(libs.ktor.client.core)
	implementation(libs.ktor.client.okhttp)
	implementation(libs.ktor.client.contentNegotiation)
	implementation(libs.ktor.serialization.json)
	implementation(libs.coil.compose)
	implementation(libs.coil.network.ktor3)
	implementation(libs.coil.gif)

	// Compose 3rd party
	implementation(libs.kmpalette.core)
	implementation(libs.kmpalette.network)
	implementation(libs.materialKolor)
	implementation(libs.composePipette)
	implementation(libs.multiplatformSettings)

	// Paging
	implementation(libs.androidx.paging.common)
	implementation(libs.androidx.paging.compose)
	implementation(libs.androidx.room3.paging)

	// Misc
	implementation(libs.koin.core)
	implementation(libs.koin.android)
	implementation(libs.koin.compose)
	implementation(libs.koin.compose.viewmodel)
	implementation(libs.koin.androidx.workmanager)
	implementation(libs.subsonicKotlin)
	implementation(libs.antisocialcord)
	ksp(libs.androidx.room3.compiler)
	implementation(libs.cryptohash)
	implementation(libs.mimetypes)
	coreLibraryDesugaring(libs.desugar.jdk.libs)

	// Testing
	testImplementation(libs.junit)
	testImplementation(libs.kotlin.test)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.ktor.client.mock)

	androidTestImplementation(libs.androidx.test.junit)
	androidTestImplementation(libs.androidx.test.runner)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(libs.kotlinx.coroutines.test)
	androidTestImplementation(libs.ktor.client.mock)
}
