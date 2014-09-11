var PushNotification = function() {};

/*
    Call this method after device Ready event is fired. This is required to send all the pending
    notifications to client.
*/
PushNotification.prototype.init = function(successCallback, errorCallback, options) {

    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
        console.log("PushNotification.init failure: failure parameter not a function");
        return
    }

    if (typeof successCallback != "function") {
        console.log("PushNotification.init failure: success callback parameter must be a function");
        return
    }

    cordova.exec(successCallback, errorCallback, "PushPlugin", "init", [options]);
};

/* 
    Call this to register for push notifications. 
    Content of [options] depends on whether we are working with APNS (iOS) or GCM (Android)
*/
PushNotification.prototype.register = function(successCallback, errorCallback, options) {

    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
        console.log("PushNotification.register failure: failure parameter not a function");
        return
    }

    if (typeof successCallback != "function") {
        console.log("PushNotification.register failure: success callback parameter must be a function");
        return
    }

    cordova.exec(successCallback, errorCallback, "PushPlugin", "register", [options]);
};

/* 
    Call this to unregister for push notifications
    This service may not work for iOS as disabling this programatically is not supported
*/
PushNotification.prototype.unregister = function(successCallback, errorCallback, options) {
    
    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
        console.log("PushNotification.unregister failure: failure parameter not a function");
        return
    }

    if (typeof successCallback != "function") {
        console.log("PushNotification.unregister failure: success callback parameter must be a function");
        return
    }

     cordova.exec(successCallback, errorCallback, "PushPlugin", "unregister", [options]);
};

/* 
    Call this to set the application icon badge
    Applicable only to iOS
*/
PushNotification.prototype.setApplicationIconBadgeNumber = function(successCallback, errorCallback, badge) {

    if (errorCallback == null) { errorCallback = function() {}}

    if (typeof errorCallback != "function")  {
        console.log("PushNotification.setApplicationIconBadgeNumber failure: failure parameter not a function");
        return
    }

    if (typeof successCallback != "function") {
        console.log("PushNotification.setApplicationIconBadgeNumber failure: success callback parameter must be a function");
        return
    }

    cordova.exec(successCallback, errorCallback, "PushPlugin", "setApplicationIconBadgeNumber", [{badge: badge}]);
};


module.exports = new PushNotification();
