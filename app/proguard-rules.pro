# Proguard rules for Nounish Launcher
-dontobfuscate
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
