# GeckoView keeps optional Google Play Services WebAuthn entry points in its published classes.
# Vola excludes that provider and ships refusing stand-ins for the classes GeckoView reaches first
# (src/full/java/com/google, passkeys option A). R8 must tolerate the remaining absent types, which
# GeckoView only touches after a stand-in has already refused the request.
-dontwarn com.google.android.gms.fido.**
-dontwarn com.google.android.gms.tasks.**
