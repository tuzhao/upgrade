# upgrade
a app upgrade library

# use step 1
add following code in your android manifest.xml.
```gradle
<provider
    android:name="android.support.v4.content.FileProvider"
    android:authorities="${applicationId}.fileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/demo_provider_paths" />
</provider>
```
create file
```gradle
pgrade/demo/src/main/res/xml/demo_provider_paths.xml
```
here is demo_provider_paths.xml content.
```gradle
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path
        name="dcim"
        path="DCIM/" />
    <external-path
        name="images"
        path="Pictures/" />
    <external-path
        name="beta_external_path"
        path="Download/" />
    <external-path
        name="beta_external_path"
        path="Pictures/" />
    <external-path
        name="external_storage_root"
        path="." />
</paths>
```

# use step 2
```java
BuildInfo.setDebugMode(true);

UpgradeInfoBean info = new UpgradeInfoBean() {
    ......
};

UpgradeDialog.Helper helper = new UpgradeDialog.Helper() {
    ......
};

boolean show = UpgradeDialog.show(this, info, helper);

UpgradeDialog.close();
```
