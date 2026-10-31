# Additional R8 rules for the ciRelease build type only, on top of the release rules.
#
# The androidTest apk doesn't bundle libraries the app already contains (like the Kotlin standard library),
# it uses the app's copy at runtime. R8 removes everything of them the app itself doesn't use,
# so keep the libraries the test runner and Espresso need. The app code is shrunk the same way as for release.

-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class androidx.tracing.** { *; }
-keep class androidx.concurrent.futures.** { *; }
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

# view ids the tests look up but the app code doesn't reference (only used in layouts),
# only R$id, keeping all R classes would keep every resource and disable the resource shrinking
-keep class es.wolfi.app.passman.R$id { public static <fields>; }

# autofill methods the tests call directly, R8 would inline them into their only caller
-keep class es.wolfi.app.passman.autofill.AutofillHelper {
    public static *** newCredentialForRequester(...);
    public static *** findMatchingCredentials(...);
}
-keep class es.wolfi.app.passman.autofill.AutofillHelper$WebDomainResult {
    public <init>();
    public void addDomain(...);
}
