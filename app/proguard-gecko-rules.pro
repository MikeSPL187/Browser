# GeckoView keeps optional Google Play Services WebAuthn entry points in its published classes.
# Vola excludes that provider, so R8 must tolerate those absent optional types.
-dontwarn com.google.android.gms.fido.**
-dontwarn com.google.android.gms.tasks.**
