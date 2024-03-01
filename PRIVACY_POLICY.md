# Zim Sync: Privacy policy

Welcome to the Zim Sync app for Android!

## Data collected by the app

This app refrains from collecting or sharing any privacy-sensitive data or personally identifiable
information.

No trackers or analytics software are installed.

It accesses locally stored media and media location data solely for the purpose of backing up that
data to a user-defined remote storage.

All user configurations are stored on the device only and can be deleted by uninstalling the app.

Data in transit is encrypted using SSL (https) unless the user opts out and configures their remote
storage differently.

## Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file:

|                 Permission                 | Why it is required                                                                                                       |
|:------------------------------------------:|--------------------------------------------------------------------------------------------------------------------------|
| `android.permission.READ_EXTERNAL_STORAGE` | Legacy permission to access photos and videos on the device.                                                             |
|   `android.permission.READ_MEDIA_IMAGES`   | New Permission to access images stored on the device.                                                                    |
|   `android.permission.READ_MEDIA_VIDEO`    | New Permission to access video stored on the device.                                                                     |
| `android.permission.ACCESS_MEDIA_LOCATION` | Permission to access the location meta-data (Exif) attached to your media files.                                         |
|       `android.permission.INTERNET`        | Permission to allow the app to access the internet for uploading your media to you the remotes storage of your choosing. |


