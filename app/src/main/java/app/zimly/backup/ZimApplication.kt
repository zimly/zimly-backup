package app.zimly.backup

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy


class ZimApplication : Application() {

    init {
        // https://stackoverflow.com/questions/56911580/w-system-a-resource-failed-to-call-release
        // TODO if dev-mode
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
