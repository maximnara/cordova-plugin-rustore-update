let RustoreUpdate = (function () {
    return {
        events: {
            // RuStore update events
            UPDATE_INFO: 'rustore-update-info',
            UPDATE_PROGRESS: 'rustore-update-progress',
            UPDATE_DOWNLOADING: 'rustore-update-downloading',
            UPDATE_INSTALLING: 'rustore-update-installing',
            INSTALL_STATE_UPDATE: 'rustore-install-state-update',
            UPDATE_FLOW_RESULT: 'rustore-update-flow-result'
        },

        /**
         * Check for app updates
         * @param {Object} params - optional parameters
         * @returns {Promise} Promise that resolves with update info
         */
        getAppUpdateInfo: function getAppUpdateInfo(params)
        {
            return new Promise((resolve, reject) => {
                params = defaults(params, {});

                callPlugin('getAppUpdateInfo', [], resolve, reject);
            });
        },

        /**
         * Helper function to check if update is available
         * @param {Object} updateInfo - update info from getAppUpdateInfo
         * @returns {Boolean} true if update is available
         */
        isUpdateAvailable: function isUpdateAvailable(updateInfo)
        {
            return updateInfo && (
                updateInfo.updateAvailability === this.UpdateAvailability.UPDATE_AVAILABLE ||
                updateInfo.updateAvailability === 'UPDATE_AVAILABLE' ||
                updateInfo.isUpdateAvailable === true
            );
        },

        /**
         * Helper function to check if update is ready to install
         * @param {Object} updateInfo - update info from getAppUpdateInfo
         * @returns {Boolean} true if update is ready to install
         */
        isUpdateReadyToInstall: function isUpdateReadyToInstall(updateInfo)
        {
            return updateInfo && (
                updateInfo.installStatus === this.InstallStatus.DOWNLOADED ||
                updateInfo.installStatus === 'DOWNLOADED' ||
                updateInfo.isUpdateReadyToInstall === true
            );
        },

        /**
         * Constants for update availability (numeric values from SDK)
         */
        UpdateAvailability: {
            UNKNOWN: 0,
            UPDATE_NOT_AVAILABLE: 1,
            UPDATE_AVAILABLE: 2,
            DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS: 3
        },

        /**
         * Constants for install status (numeric values from SDK)
         */
        InstallStatus: {
            UNKNOWN: 0,
            PENDING: 1,
            DOWNLOADING: 2,
            DOWNLOADED: 11,
            INSTALLING: 3,
            INSTALLED: 4,
            FAILED: 5,
            CANCELED: 6
        },

        /**
         * Text representations for update availability
         */
        UpdateAvailabilityText: {
            0: 'UNKNOWN',
            1: 'UPDATE_NOT_AVAILABLE',
            2: 'UPDATE_AVAILABLE',
            3: 'DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS'
        },

        /**
         * Text representations for install status
         */
        InstallStatusText: {
            0: 'UNKNOWN',
            1: 'PENDING',
            2: 'DOWNLOADING',
            3: 'INSTALLING',
            4: 'INSTALLED',
            5: 'FAILED',
            6: 'CANCELED',
            11: 'DOWNLOADED'
        },

        /**
         * Constants for update types
         */
        UpdateType: {
            FLEXIBLE: 'FLEXIBLE',
            IMMEDIATE: 'IMMEDIATE'
        },

        /**
         * Constants for activity result codes
         */
        ActivityResult: {
            RESULT_OK: -1,
            RESULT_CANCELED: 0,
            RESULT_IN_APP_UPDATE_FAILED: 1
        },

        /**
         * Result messages for update flow
         */
        UpdateFlowResult: {
            IMMEDIATE_UPDATE_SUCCESS: 'IMMEDIATE_UPDATE_SUCCESS',
            UPDATE_CANCELED_BY_USER: 'UPDATE_CANCELED_BY_USER',
            UPDATE_DOWNLOAD_FAILED: 'UPDATE_DOWNLOAD_FAILED'
        },

        /**
         * Error codes for update flow failures
         */
        UpdateFlowError: {
            UPDATE_FLOW_FAILED: 'UPDATE_FLOW_FAILED',
            UPDATE_FLOW_START_ERROR: 'UPDATE_FLOW_START_ERROR',
            UPDATE_CHECK_FAILED: 'UPDATE_CHECK_FAILED',
            UPDATE_FLOW_ERROR: 'UPDATE_FLOW_ERROR',
            MANAGER_CREATE_FAILED: 'MANAGER_CREATE_FAILED',
            UPDATE_COMPLETE_FAILED: 'UPDATE_COMPLETE_FAILED',
            UPDATE_COMPLETE_ERROR: 'UPDATE_COMPLETE_ERROR'
        },

        /**
         * Update flow status messages
         */
        UpdateFlowStatus: {
            UPDATE_DOWNLOADING: 'update_downloading',
            UPDATE_INSTALLING: 'update_installing',
            DOWNLOADING: 'downloading'
        },

        /**
         * Helper function to check if update was canceled by user
         * @param {Object} result - result from startUpdateFlow
         * @returns {Boolean} true if user canceled the update
         */
        isUpdateCanceled: function isUpdateCanceled(result)
        {
            return result && (
                result.resultCode === this.ActivityResult.RESULT_CANCELED ||
                result.resultMessage === this.UpdateFlowResult.UPDATE_CANCELED_BY_USER
            );
        },

        /**
         * Helper function to check if update flow was successful
         * @param {Object} result - result from startUpdateFlow
         * @returns {Boolean} true if update flow was successful
         */
        isUpdateSuccessful: function isUpdateSuccessful(result)
        {
            return result && (
                result.resultCode === this.ActivityResult.RESULT_OK ||
                result.resultMessage === this.UpdateFlowResult.IMMEDIATE_UPDATE_SUCCESS
            );
        },

        /**
         * Helper function to check if update flow failed
         * @param {Object} error - error from startUpdateFlow
         * @returns {Boolean} true if update flow failed
         */
        isUpdateFailed: function isUpdateFailed(error)
        {
            return error && (
                error.error ||
                error.resultCode === this.ActivityResult.RESULT_IN_APP_UPDATE_FAILED ||
                error.resultMessage === this.UpdateFlowResult.UPDATE_DOWNLOAD_FAILED
            );
        },

        /**
         * Start update flow to download/install the update
         * @param {Object} params - update flow parameters
         * @param {String} params.updateType - type of update (FLEXIBLE, IMMEDIATE, or SILENT)
         * @returns {Promise} Promise that resolves with update flow result
         */
        startUpdateFlow: function startUpdateFlow(params)
        {
            return new Promise((resolve, reject) => {
                params = defaults(params, {
                    updateType: 'FLEXIBLE'
                });

                // Validate update type
                const validTypes = ['FLEXIBLE', 'IMMEDIATE'];
                const updateType = (params.updateType || 'FLEXIBLE').toUpperCase();

                if (!validTypes.includes(updateType)) {
                    reject({
                        code: 'INVALID_UPDATE_TYPE',
                        message: 'Invalid update type. Use FLEXIBLE or IMMEDIATE.'
                    });
                    return;
                }

                callPlugin('startUpdateFlow', [{ updateType: updateType }], resolve, reject);
            });
        },
    }
})();



/**
 * Helper function to call cordova plugin
 * @param {String} name - function name to call
 * @param {Array} params - optional params
 * @param {Function} onSuccess - optional on success function
 * @param {Function} onFailure - optional on failure function
 */
function callPlugin(name, params, onSuccess, onFailure)
{
    cordova.exec(function callPluginSuccess(result)
    {
        if (isFunction(onSuccess))
        {
            onSuccess(result);
        }
    }, function callPluginFailure(error)
    {
        if (isFunction(onFailure))
        {
            onFailure(error)
        }
    }, 'RustoreUpdatePlugin', name, params);
}

/**
 * Helper function to check if a function is a function
 * @param {Object} functionToCheck - function to check if is function
 */
function isFunction(functionToCheck)
{
    var getType = {};
    var isFunction = functionToCheck && getType.toString.call(functionToCheck) === '[object Function]';
    return isFunction === true;
}

/**
 * Helper function to do a shallow defaults (merge). Does not create a new object, simply extends it
 * @param {Object} o - object to extend
 * @param {Object} defaultObject - defaults to extend o with
 */
function defaults(o, defaultObject)
{
    if (typeof o === 'undefined')
    {
        return defaults({}, defaultObject);
    }

    for (var j in defaultObject)
    {
        if (defaultObject.hasOwnProperty(j) && o.hasOwnProperty(j) === false)
        {
            o[j] = defaultObject[j];
        }
    }

    return o;
}


if (typeof module !== undefined && module.exports)
{
    module.exports = RustoreUpdate;
}