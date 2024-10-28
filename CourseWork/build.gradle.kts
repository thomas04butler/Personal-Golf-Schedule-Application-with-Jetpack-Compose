plugins {
    // Add the necessary plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

// Other configurations if needed
allprojects {
    // No repositories block here
    // You can add other configurations if needed
}

// If you have specific configurations for subprojects or the root project,
// add them in the corresponding blocks. For example:
subprojects {
    // Subproject-specific configurations
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
