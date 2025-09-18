package com.maximnara.rustore.update

import com.maximnara.rustore.update.helpers.BaseHelper
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CallbackContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.content.Context
import android.util.Log
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateInfo
import ru.rustore.sdk.appupdate.model.UpdateAvailability
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.model.InstallState
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import android.app.Activity
import org.apache.cordova.PluginResult

class RustoreUpdatePlugin : CordovaPlugin() {

    private lateinit var helper: BaseHelper
    private var installStateListener: InstallStateUpdateListener? = null
    private var currentUpdateInfo: AppUpdateInfo? = null
    private var currentManager: RuStoreAppUpdateManager? = null

    companion object {
        private const val TAG = "RustoreUpdatePlugin"
    }

    override fun initialize(cordova: org.apache.cordova.CordovaInterface, webView: org.apache.cordova.CordovaWebView) {
        super.initialize(cordova, webView)
        helper = PluginHelper(this, webView)
    }

    override fun onPause(multitasking: Boolean) {
        super.onPause(multitasking)
        try {
            // Temporarily unregister listener when app is paused
            currentManager?.let { manager ->
                installStateListener?.let {
                    try {
                        manager.unregisterListener(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unregistering listener in onPause: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause: ${e.message}")
        }
    }

    override fun onResume(multitasking: Boolean) {
        super.onResume(multitasking)
        try {
            // Re-register listener when app resumes
            currentManager?.let { manager ->
                installStateListener?.let {
                    try {
                        manager.registerListener(it)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error registering listener in onResume: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            // Safely unbind service if manager exists
            currentManager?.let { manager ->
                try {
                    // Unregister listener if exists
                    installStateListener?.let {
                        manager.unregisterListener(it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering listener in onStop: ${e.message}")
                }
                currentManager = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            // Final cleanup
            currentManager?.let { manager ->
                try {
                    installStateListener?.let {
                        manager.unregisterListener(it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error unregistering listener in onDestroy: ${e.message}")
                }
            }

            // Clear all references
            currentManager = null
            installStateListener = null
            currentUpdateInfo = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
        super.onDestroy()
    }

    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        return try {
            when (action) {
                "getAppUpdateInfo" -> {
                    getAppUpdateInfo(args, callbackContext)
                    true
                }
                "startUpdateFlow" -> {
                    startUpdateFlow(args, callbackContext)
                    true
                }
                // Add your plugin methods here
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                    callbackContext.error("Unknown action: $action")
                    false
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSON Exception: ${e.message}")
            callbackContext.error("JSON Exception: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            callbackContext.error("Exception: ${e.message}")
            false
        }
    }

    // Add your plugin methods here

    private fun getAppUpdateInfo(args: JSONArray, callbackContext: CallbackContext) {
        cordova.threadPool.execute {
            try {
                helper.log("Checking for app updates", "")

                // Clear previous manager if exists
                currentManager = null

                // Create a new manager instance for this call
                val context = cordova.activity.applicationContext
                val manager = RuStoreAppUpdateManagerFactory.create(context)

                if (manager == null) {
                    val errorResult = JSONObject().apply {
                                        put("error", "MANAGER_CREATE_FAILED")
                                        put("message", "Failed to create update manager")
                                    }
                                    callbackContext.error(errorResult)
                    return@execute
                }

                // Store reference for proper cleanup
                currentManager = manager

                manager.getAppUpdateInfo()
                    ?.addOnSuccessListener { appUpdateInfo ->
                        helper.log("Update check completed", appUpdateInfo.updateAvailability.toString())

                        // Store the update info for use in startUpdateFlow
                        currentUpdateInfo = appUpdateInfo

                        val result = JSONObject().apply {
                            put("updateAvailability", appUpdateInfo.updateAvailability)
                            put("installStatus", appUpdateInfo.installStatus)
                            put("availableVersionCode", appUpdateInfo.availableVersionCode)
                            put("isUpdateAvailable", appUpdateInfo.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE)
                            put("isUpdateReadyToInstall", appUpdateInfo.installStatus == InstallStatus.DOWNLOADED)

                            // Add mapped values for convenience
                            put("updateAvailabilityText", mapUpdateAvailability(appUpdateInfo.updateAvailability))
                            put("installStatusText", mapInstallStatus(appUpdateInfo.installStatus))
                        }

                        // Send success event
                        sendEvent("rustore-update-info", result)

                        // Return raw result directly
                        callbackContext.success(result)
                    }
                    ?.addOnFailureListener { exception ->
                        helper.log("Update check failed", exception.message)
                    val errorResult = JSONObject().apply {
                        put("error", "UPDATE_CHECK_FAILED")
                        put("message", exception.message ?: "Update check failed")
                    }
                    callbackContext.error(errorResult)
                    }

            } catch (e: Exception) {
                helper.log("Error checking updates", e.message)
                val errorResult = JSONObject().apply {
                    put("error", "UPDATE_CHECK_ERROR")
                    put("message", e.message ?: "Error checking updates")
                }
                callbackContext.error(errorResult)
            }
        }
    }

    private fun mapUpdateAvailability(availability: Int): String {
        return when (availability) {
            UpdateAvailability.UPDATE_AVAILABLE -> "UPDATE_AVAILABLE"
            UpdateAvailability.UPDATE_NOT_AVAILABLE -> "UPDATE_NOT_AVAILABLE"
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> "DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS"
            UpdateAvailability.UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    private fun mapInstallStatus(status: Int): String {
        return when (status) {
            InstallStatus.DOWNLOADED -> "DOWNLOADED"
            InstallStatus.DOWNLOADING -> "DOWNLOADING"
            InstallStatus.FAILED -> "FAILED"
            InstallStatus.PENDING -> "PENDING"
            InstallStatus.INSTALLING -> "INSTALLING"
            InstallStatus.UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    private fun startUpdateFlow(args: JSONArray, callbackContext: CallbackContext) {
        cordova.threadPool.execute {
            try {
                // Get update type from arguments
                val options = if (args.length() > 0) args.getJSONObject(0) else JSONObject()
                val updateTypeString = options.optString("updateType", "FLEXIBLE")

                val updateType = when (updateTypeString.uppercase()) {
                    "IMMEDIATE" -> AppUpdateType.IMMEDIATE
                    "FLEXIBLE" -> AppUpdateType.FLEXIBLE
                    else -> AppUpdateType.FLEXIBLE
                }

                helper.log("Starting update flow", "Update type: $updateTypeString")

                // Clear previous manager if exists
                currentManager = null

                // Create a new manager instance
                val context = cordova.activity.applicationContext
                val manager = RuStoreAppUpdateManagerFactory.create(context)

                if (manager == null) {
                    val errorResult = JSONObject().apply {
                                        put("error", "MANAGER_CREATE_FAILED")
                                        put("message", "Failed to create update manager")
                                    }
                                    callbackContext.error(errorResult)
                    return@execute
                }

                // Store reference for proper cleanup
                currentManager = manager

                // Get update info first
                manager.getAppUpdateInfo()
                    ?.addOnSuccessListener { appUpdateInfo ->
                        helper.log("Update check completed", appUpdateInfo.updateAvailability.toString())

                        currentUpdateInfo = appUpdateInfo

                        // Build update options
                        val appUpdateOptions = AppUpdateOptions.Builder()
                            .appUpdateType(updateType)
                            .build()

                        // Start the update flow on UI thread
                        cordova.activity.runOnUiThread {
                            try {
                                manager.startUpdateFlow(appUpdateInfo, appUpdateOptions)
                                    ?.addOnSuccessListener { resultCode ->
                                        helper.log("Update flow completed", "Result code: $resultCode")

                                        when (resultCode) {
                                            Activity.RESULT_OK -> {
                                                // User accepted the update
                                                if (updateType == AppUpdateType.FLEXIBLE) {
                                                    // For flexible updates, we need to check the actual download state
                                                    // Register listener to track download/install state
                                                    var isListenerActive = true
                                                    val downloadListener = object : InstallStateUpdateListener {
                                                        override fun onStateUpdated(state: InstallState) {
                                                            helper.log("Install state updated", "Status: ${state.installStatus}, Downloaded: ${state.bytesDownloaded}, Total: ${state.totalBytesToDownload}")

                                                            when (state.installStatus) {
                                                                InstallStatus.DOWNLOADED -> {
                                                                    // Update downloaded, start installation automatically
                                                                    if (isListenerActive) {
                                                                        isListenerActive = false
                                                                        cordova.activity.runOnUiThread {
                                                                            completeUpdateInstallation(manager, callbackContext)
                                                                        }
                                                                        // Unregister this temporary listener
                                                                        manager.unregisterListener(this)
                                                                    }
                                                                }
                                                                InstallStatus.DOWNLOADING -> {
                                                                    // Update is downloading, send progress event
                                                                    val progress = if (state.totalBytesToDownload > 0) {
                                                                        (state.bytesDownloaded * 100 / state.totalBytesToDownload).toInt()
                                                                    } else {
                                                                        0
                                                                    }
                                                                    val progressEvent = JSONObject().apply {
                                                                        put("status", "downloading")
                                                                        put("progress", progress)
                                                                        put("bytesDownloaded", state.bytesDownloaded)
                                                                        put("totalBytesToDownload", state.totalBytesToDownload)
                                                                    }
                                                                    sendEvent("rustore-update-progress", progressEvent)
                                                                }
                                                                InstallStatus.FAILED -> {
                                                                    // Download failed
                                                                    if (isListenerActive) {
                                                                        isListenerActive = false
                                                                        val errorResult = JSONObject().apply {
                                                                            put("resultCode", resultCode)
                                                                            put("resultMessage", "UPDATE_DOWNLOAD_FAILED")
                                                                            put("installErrorCode", state.installErrorCode)
                                                                        }
                                                                        callbackContext.error(errorResult)
                                                                        manager.unregisterListener(this)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    manager.registerListener(downloadListener)

                                                    // Check current state immediately after registering listener
                                                    // This handles the case where update is already downloaded
                                                    manager.getAppUpdateInfo()?.addOnSuccessListener { currentInfo ->
                                                        if (currentInfo.installStatus == InstallStatus.DOWNLOADED && isListenerActive) {
                                                            // Update already downloaded, install it immediately
                                                            isListenerActive = false
                                                            cordova.activity.runOnUiThread {
                                                                completeUpdateInstallation(manager, callbackContext)
                                                            }
                                                            // Unregister the listener as it's no longer needed
                                                            manager.unregisterListener(downloadListener)
                                                        } else {
                                                            // Send event that update is downloading
                                                            val downloadingEvent = JSONObject().apply {
                                                                put("status", "update_downloading")
                                                                put("message", "Update download started")
                                                            }
                                                            sendEvent("rustore-update-downloading", downloadingEvent)
                                                        }
                                                    }
                                                } else if (updateType == AppUpdateType.IMMEDIATE) {
                                                    // For immediate update, the app will restart after successful update
                                                    // This code may not be reached
                                                    val result = JSONObject().apply {
                                                        put("resultCode", resultCode)
                                                        put("resultMessage", "IMMEDIATE_UPDATE_SUCCESS")
                                                        put("updateType", updateTypeString)
                                                    }
                                                    callbackContext.success(result)
                                                }
                                            }
                                            Activity.RESULT_CANCELED -> {
                                                // User canceled the update
                                                val result = JSONObject().apply {
                                                    put("resultCode", resultCode)
                                                    put("resultMessage", "UPDATE_CANCELED_BY_USER")
                                                    put("updateType", updateTypeString)
                                                }
                                                callbackContext.error(result)
                                            }
                                            else -> {
                                                // Update failed
                                                val result = JSONObject().apply {
                                                    put("resultCode", resultCode)
                                                    put("resultMessage", mapUpdateResultCode(resultCode))
                                                    put("updateType", updateTypeString)
                                                }
                                                callbackContext.error(result)
                                            }
                                        }
                                    }
                                    ?.addOnFailureListener { exception ->
                                        helper.log("Update flow failed", exception.message)
                                        val errorResult = JSONObject().apply {
                                            put("error", "UPDATE_FLOW_FAILED")
                                            put("message", exception.message ?: "Unknown error")
                                        }
                                        callbackContext.error(errorResult)
                                    }
                            } catch (e: Exception) {
                                helper.log("Error starting update flow", e.message)
                                val errorResult = JSONObject().apply {
                                    put("error", "UPDATE_FLOW_START_ERROR")
                                    put("message", e.message ?: "Unknown error")
                                }
                                callbackContext.error(errorResult)
                            }
                        }
                    }
                    ?.addOnFailureListener { exception ->
                        helper.log("Update check failed", exception.message)
                        val errorResult = JSONObject().apply {
                            put("error", "UPDATE_CHECK_FAILED")
                            put("message", exception.message ?: "Unknown error")
                        }
                        callbackContext.error(errorResult)
                    }

            } catch (e: Exception) {
                helper.log("Error in startUpdateFlow", e.message)
                val errorResult = JSONObject().apply {
                    put("error", "UPDATE_FLOW_ERROR")
                    put("message", e.message ?: "Unknown error")
                }
                callbackContext.error(errorResult)
            }
        }
    }

    private fun mapUpdateResultCode(resultCode: Int): String {
        return when (resultCode) {
            Activity.RESULT_OK -> "RESULT_OK"
            Activity.RESULT_CANCELED -> "RESULT_CANCELED"
            1 -> "RESULT_IN_APP_UPDATE_FAILED"
            else -> "RESULT_UNKNOWN_$resultCode"
        }
    }

    private fun completeUpdateInstallation(manager: RuStoreAppUpdateManager, callbackContext: CallbackContext) {
        try {
            helper.log("Starting automatic update installation", "")

            // Build update options for complete update
            val appUpdateOptions = AppUpdateOptions.Builder()
                .appUpdateType(AppUpdateType.FLEXIBLE)
                .build()

            // Call completeUpdate to install the downloaded update
            manager.completeUpdate(appUpdateOptions)
                ?.addOnSuccessListener {
                    helper.log("Complete update initiated successfully", "")

                    val result = JSONObject().apply {
                        put("status", "update_installing")
                        put("message", "Update installation started automatically")
                    }

                    sendEvent("rustore-update-installing", result)
                    callbackContext.success(result)
                }
                ?.addOnFailureListener { exception ->
                    helper.log("Failed to complete update", exception.message)
                    val errorResult = JSONObject().apply {
                        put("error", "UPDATE_COMPLETE_FAILED")
                        put("message", exception.message ?: "Failed to complete update")
                    }
                    callbackContext.error(errorResult)
                }

        } catch (e: Exception) {
            helper.log("Error completing update", e.message)
            val errorResult = JSONObject().apply {
                put("error", "UPDATE_COMPLETE_ERROR")
                put("message", e.message ?: "Error completing update")
            }
            callbackContext.error(errorResult)
        }
    }

    // Helper method to send events to JavaScript
    private fun sendEvent(eventName: String, data: JSONObject? = null) {
        helper.emitWindowEvent(eventName, data)
    }
    
    // Inner helper class that extends BaseHelper
    private class PluginHelper(cordovaPlugin: CordovaPlugin, cordovaWebView: org.apache.cordova.CordovaWebView) 
        : BaseHelper(cordovaPlugin, cordovaWebView)
}