plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    //id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.apptrackerprogress"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.apptrackerprogress"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Para pruebas: firma el release con la llave de debug y así se puede instalar
            // directamente en el celular. Para publicar en Play Store se usa una llave propia.
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Permite usar clases de Firebase (ej. Timestamp) en los tests unitarios de la lógica
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    // Firebase BoM (Bill of Materials para manejar versiones)
    //implementation(platform("com.google.firebase:firebase-bom:32.8.0"))

    // Auth y Firestore
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Navegación en Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Credential Manager para el login con Google moderno
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    //implementacion para que funcionen los import de iconos
    implementation ("androidx.compose.material:material-icons-extended")

    //implementacion para poder cargar fotos de perfil
    implementation("io.coil-kt:coil-compose:2.6.0")

    //implementacion para el storage de fotos y videos
    implementation("com.google.firebase:firebase-storage")

    // Corrutinas para Firebase (.await()) usadas por los repositorios
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    //implementacion para el storage con cloudinary
    implementation("com.cloudinary:cloudinary-android:2.5.0")
}