-keep class com.yishell.app.data.model.** { *; }
-keep class org.connectbot.** { *; }
-dontwarn org.connectbot.**

# Tink crypto library (required by sshlib 2.2.48+ for Curve25519/X25519 key exchange)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
