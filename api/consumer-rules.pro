# DTO 是插件 ABI 的 wire 契约。混淆必须保留类名、字段名与字段。
-keep class com.stupidtree.hita.api.dto.** { *; }
-keepclassmembers class com.stupidtree.hita.api.dto.** {
    <fields>;
}
