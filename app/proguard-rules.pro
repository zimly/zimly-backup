# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Got some inspiration from here:
# https://stackoverflow.com/a/51083930

# Print removed classes to file
#-printusage ./usage.txt

-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.beans.ConstructorProperties

-keepattributes Signature, *Annotation*

# Minio does a lot of XML magic
-keep class io.minio.** { *; }

# SAX stuff
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-keep class com.ctc.wstx.sax.WstxSAXParserFactory

# Simple XML
-dontwarn org.simpleframework.xml.stream.**
-keep public class org.simpleframework.** { *; }
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.core.** { *; }
-keep class org.simpleframework.xml.util.** { *; }

-keepclassmembers,allowobfuscation class * {
  @org.simpleframework.xml.* <init>(...);
  @org.simpleframework.xml.* <fields>;
}
