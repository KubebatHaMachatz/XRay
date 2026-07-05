package com.demo.xray.engine.apk.manifest

import com.demo.xray.core.model.PermissionRiskCategory

/**
 * Bundled, versioned, offline permission knowledge base. See PRD FR-043. Deliberately small for
 * this milestone (a curated set of the most consequential platform permissions); unknown
 * permissions still display correctly by full name with [PermissionRiskCategory.UNKNOWN] rather
 * than a guessed category. Extend this table rather than special-casing callers.
 */
object PermissionKnowledgeBase {
    const val VERSION = "2026.07.0"

    data class Entry(
        val friendlyName: String,
        val group: String?,
        val riskCategory: PermissionRiskCategory,
        val explanation: String,
    )

    private val entries: Map<String, Entry> =
        mapOf(
            "android.permission.INTERNET" to Entry("Internet access", "Network", PermissionRiskCategory.NORMAL, "Open network sockets."),
            "android.permission.ACCESS_NETWORK_STATE" to Entry("View network state", "Network", PermissionRiskCategory.NORMAL, "View network connectivity information."),
            "android.permission.ACCESS_WIFI_STATE" to Entry("View Wi-Fi state", "Network", PermissionRiskCategory.NORMAL, "View Wi-Fi connectivity information."),
            "android.permission.VIBRATE" to Entry("Control vibration", "Device", PermissionRiskCategory.NORMAL, "Control the vibrator."),
            "android.permission.WAKE_LOCK" to Entry("Prevent device from sleeping", "Device", PermissionRiskCategory.NORMAL, "Prevent the device from dimming or sleeping."),
            "android.permission.RECEIVE_BOOT_COMPLETED" to Entry("Run at startup", "Device", PermissionRiskCategory.NORMAL, "Start automatically after the device finishes booting."),
            "android.permission.BLUETOOTH" to Entry("Bluetooth access", "Connectivity", PermissionRiskCategory.NORMAL, "Connect to paired Bluetooth devices."),
            "android.permission.NFC" to Entry("NFC access", "Connectivity", PermissionRiskCategory.NORMAL, "Communicate with NFC tags/devices."),
            "android.permission.CAMERA" to Entry("Take pictures and video", "Camera", PermissionRiskCategory.SENSITIVE, "Access the camera hardware."),
            "android.permission.RECORD_AUDIO" to Entry("Record audio", "Microphone", PermissionRiskCategory.SENSITIVE, "Access the microphone."),
            "android.permission.ACCESS_FINE_LOCATION" to Entry("Precise location", "Location", PermissionRiskCategory.SENSITIVE, "Access precise device location."),
            "android.permission.ACCESS_COARSE_LOCATION" to Entry("Approximate location", "Location", PermissionRiskCategory.SENSITIVE, "Access approximate device location."),
            "android.permission.ACCESS_BACKGROUND_LOCATION" to Entry("Background location", "Location", PermissionRiskCategory.SENSITIVE, "Access location while the app is in the background."),
            "android.permission.READ_CONTACTS" to Entry("Read contacts", "Contacts", PermissionRiskCategory.SENSITIVE, "Read the user's contacts."),
            "android.permission.WRITE_CONTACTS" to Entry("Write contacts", "Contacts", PermissionRiskCategory.SENSITIVE, "Modify the user's contacts."),
            "android.permission.READ_CALENDAR" to Entry("Read calendar", "Calendar", PermissionRiskCategory.SENSITIVE, "Read the user's calendar events."),
            "android.permission.WRITE_CALENDAR" to Entry("Write calendar", "Calendar", PermissionRiskCategory.SENSITIVE, "Modify the user's calendar events."),
            "android.permission.READ_SMS" to Entry("Read SMS", "SMS", PermissionRiskCategory.SENSITIVE, "Read text messages."),
            "android.permission.RECEIVE_SMS" to Entry("Receive SMS", "SMS", PermissionRiskCategory.SENSITIVE, "Receive text messages."),
            "android.permission.SEND_SMS" to Entry("Send SMS", "SMS", PermissionRiskCategory.SENSITIVE, "Send text messages, which may incur charges."),
            "android.permission.READ_CALL_LOG" to Entry("Read call log", "Phone", PermissionRiskCategory.SENSITIVE, "Read the device's call history."),
            "android.permission.WRITE_CALL_LOG" to Entry("Write call log", "Phone", PermissionRiskCategory.SENSITIVE, "Modify the device's call history."),
            "android.permission.CALL_PHONE" to Entry("Place calls", "Phone", PermissionRiskCategory.SENSITIVE, "Initiate phone calls without confirmation."),
            "android.permission.READ_PHONE_STATE" to Entry("Read phone state", "Phone", PermissionRiskCategory.SENSITIVE, "Read device identifiers and call state."),
            "android.permission.BODY_SENSORS" to Entry("Body sensors", "Sensors", PermissionRiskCategory.SENSITIVE, "Access biometric sensor data."),
            "android.permission.ACTIVITY_RECOGNITION" to Entry("Physical activity", "Sensors", PermissionRiskCategory.SENSITIVE, "Access physical activity data."),
            "android.permission.POST_NOTIFICATIONS" to Entry("Post notifications", "Notifications", PermissionRiskCategory.SENSITIVE, "Show notifications to the user."),
            "android.permission.READ_EXTERNAL_STORAGE" to Entry("Read shared storage", "Storage", PermissionRiskCategory.SENSITIVE, "Read files from shared storage."),
            "android.permission.WRITE_EXTERNAL_STORAGE" to Entry("Write shared storage", "Storage", PermissionRiskCategory.SENSITIVE, "Write files to shared storage."),
            "android.permission.ACCESS_MEDIA_LOCATION" to Entry("Access media location", "Storage", PermissionRiskCategory.SENSITIVE, "Access geographic location embedded in media files."),
            "android.permission.SYSTEM_ALERT_WINDOW" to Entry("Draw over other apps", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Display content on top of other apps."),
            "android.permission.WRITE_SETTINGS" to Entry("Modify system settings", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Modify system settings."),
            "android.permission.REQUEST_INSTALL_PACKAGES" to Entry("Install unknown apps", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Request installation of other packages."),
            "android.permission.PACKAGE_USAGE_STATS" to Entry("App usage access", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Access usage statistics of other apps."),
            "android.permission.BIND_ACCESSIBILITY_SERVICE" to Entry("Accessibility service", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Provide an accessibility service with broad UI access."),
            "android.permission.MANAGE_EXTERNAL_STORAGE" to Entry("Manage all files", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Broad read/write access to shared storage."),
            "android.permission.QUERY_ALL_PACKAGES" to Entry("Query all installed apps", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "See the full list of installed apps on the device."),
            "android.permission.BIND_DEVICE_ADMIN" to Entry("Device admin", "Special access", PermissionRiskCategory.PRIVILEGED_SYSTEM, "Full device administration policy control."),
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to Entry("Notification access", "Special access", PermissionRiskCategory.SPECIAL_ACCESS, "Read and dismiss the user's notifications."),
        )

    fun lookup(permissionName: String): Entry? = entries[permissionName]
}
