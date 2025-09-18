package com.maximnara.rustore.update.helpers

import android.util.Log
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONObject

internal abstract class BaseHelper(
    protected val cordovaPlugin: CordovaPlugin,
    protected val cordovaWebView: CordovaWebView
) {
    protected val cordova = cordovaPlugin.cordova

    companion object {
        private const val FIRE_WINDOW_EVENT = "javascript:cordova.fireWindowEvent('%s');"
        private const val FIRE_WINDOW_EVENT_WITH_DATA = "javascript:cordova.fireWindowEvent('%s', %s);"
        protected const val TAG = "RustoreUpdatePlugin"
    }

    /**
     * Emits a window event to JavaScript
     * @param event Event name
     * @param data Optional data to send with the event
     * @param logEventDescription Optional description for logging
     */
    fun emitWindowEvent(
        event: String, 
        data: JSONObject? = null, 
        logEventDescription: String? = null
    ) {
        log(event, logEventDescription)

        cordovaPlugin.cordova.activity.runOnUiThread {
            val jsCode = if (data != null) {
                String.format(FIRE_WINDOW_EVENT_WITH_DATA, event, data.toString())
            } else {
                String.format(FIRE_WINDOW_EVENT, event)
            }
            cordovaWebView.loadUrl(jsCode)
        }
    }

    /**
     * Logs an event with optional description
     * @param event Event name
     * @param logEventDescription Optional description
     */
    fun log(event: String, logEventDescription: String? = null) {
        val logEvent = if (logEventDescription != null) {
            "$event: $logEventDescription"
        } else {
            event
        }
        Log.d(TAG, logEvent)
    }

    /**
     * Helper method to create success response
     * @param message Success message
     * @param data Optional data to include
     */
    fun createSuccessResult(message: String, data: Any? = null): JSONObject {
        val result = JSONObject()
        result.put("success", true)
        result.put("message", message)
        if (data != null) {
            result.put("data", data)
        }
        return result
    }

    /**
     * Helper method to create error response
     * @param message Error message
     * @param error Optional error details
     */
    fun createErrorResult(message: String, error: Any? = null): JSONObject {
        val result = JSONObject()
        result.put("success", false)
        result.put("message", message)
        if (error != null) {
            result.put("error", error)
        }
        return result
    }

    /**
     * Helper method to safely execute callback with success result
     * @param callbackContext Cordova callback context
     * @param message Success message
     * @param data Optional data
     */
    fun callbackSuccess(
        callbackContext: CallbackContext,
        message: String,
        data: Any? = null
    ) {
        try {
            val result = createSuccessResult(message, data)
            callbackContext.success(result)
        } catch (e: Exception) {
            log("callbackSuccess error", e.message)
            callbackContext.error("Callback error: ${e.message}")
        }
    }

    /**
     * Helper method to safely execute callback with error result
     * @param callbackContext Cordova callback context
     * @param message Error message
     * @param error Optional error details
     */
    fun callbackError(
        callbackContext: CallbackContext,
        message: String,
        error: Any? = null
    ) {
        try {
            val result = createErrorResult(message, error)
            callbackContext.error(result)
        } catch (e: Exception) {
            log("callbackError error", e.message)
            callbackContext.error("Callback error: ${e.message}")
        }
    }

    /**
     * Helper method to check if plugin is initialized
     * @param isInitialized Current initialization state
     * @param callbackContext Callback context for error response
     * @return true if initialized, false otherwise
     */
    fun checkInitialized(
        isInitialized: Boolean,
        callbackContext: CallbackContext
    ): Boolean {
        if (!isInitialized) {
            callbackError(callbackContext, "Plugin not initialized")
            return false
        }
        return true
    }
}