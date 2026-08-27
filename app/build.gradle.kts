plugins {
    id("com.android.application")
}
android {
    namespace="com.skynet.v380mvr"
    compileSdk=36
    defaultConfig {
        applicationId="com.skynet.v380mvr"
        minSdk=24
        targetSdk=36
        versionCode=1
        versionName="1.0"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.media3:media3-exoplayer:1.11.0")
    implementation("androidx.media3:media3-ui:1.11.0")
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7")
}
