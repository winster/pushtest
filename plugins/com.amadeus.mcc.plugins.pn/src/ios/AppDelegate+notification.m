//
//  AppDelegate+notification.m
//  pushtest
//
//  Created by Robert Easterday on 10/26/12.
//
//

#import "AppDelegate+notification.h"
#import "PushPlugin.h"
#import <objc/runtime.h>

static char launchNotificationKey;

@implementation AppDelegate (notification)

- (id) getCommandInstance:(NSString*)className {
    return [self.viewController getCommandInstance:className];
}

// its dangerous to override a method from within a category.
// Instead we will use method swizzling. we set this up in the load call.
+ (void)load {
    Method original, swizzled;
    
    original = class_getInstanceMethod(self, @selector(init));
    swizzled = class_getInstanceMethod(self, @selector(swizzled_init));
    method_exchangeImplementations(original, swizzled);
}

- (AppDelegate *)swizzled_init {
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(createNotificationChecker:)
               name:@"UIApplicationDidFinishLaunchingNotification" object:nil];
    
    // This actually calls the original init method over in AppDelegate. Equivilent to calling super
    // on an overrided method, this is not recursive, although it appears that way. neat huh?
    return [self swizzled_init];
}

// This code will be called immediately after application:didFinishLaunchingWithOptions:. We need
// to process notifications in cold-start situations
- (void)createNotificationChecker:(NSNotification *)notification {
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    if (notification) {
        NSDictionary *launchOptions = [notification userInfo];
        if (launchOptions) {
            pushHandler.messageLog = @"launchOptions received";
            NSDictionary *launchNotification = [launchOptions objectForKey: @"UIApplicationLaunchOptionsRemoteNotificationKey"];
            [pushHandler.pendingNotifications addObject:launchNotification];
        } else {
            pushHandler.messageLog = @"no launchoptions";
        }
    } else {
        pushHandler.messageLog = @"no notification";
    }
}
/*- (BOOL)application:(UIApplication*)application willFinishLaunchingWithOptions:(NSDictionary*)launchOptions {
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    if (launchOptions) {
        pushHandler.messageLog = @"launchOptions received willfinish";
        NSDictionary *launchNotification = [launchOptions objectForKey: @"UIApplicationLaunchOptionsRemoteNotificationKey"];
        [pushHandler.pendingNotifications addObject:launchNotification];
    } else {
        pushHandler.messageLog = @"no launchoptions willfinish";
    }
    return YES;
}*/

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    [pushHandler didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    [pushHandler didFailToRegisterForRemoteNotificationsWithError:error];
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSLog(@"didReceiveNotification");
    
    // Get application state for iOS4.x+ devices, otherwise assume active
    UIApplicationState appState = UIApplicationStateActive;
    if ([application respondsToSelector:@selector(applicationState)]) {
        appState = application.applicationState;
    }
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    
    if (appState == UIApplicationStateActive) {
        pushHandler.notificationMessage = userInfo;
        pushHandler.isInline = YES;
        [pushHandler notificationReceived : userInfo];
    } else {
        //save it for later
        //self.launchNotification = userInfo;
        [pushHandler.pendingNotifications addObject:userInfo];
    }
}

/*- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
    NSLog(@"didReceiveNotification FetchCompletion Handler");
    
        // Get application state for iOS4.x+ devices, otherwise assume active
    UIApplicationState appState = UIApplicationStateActive;
    if ([application respondsToSelector:@selector(applicationState)]) {
        appState = application.applicationState;
    }
    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    pushHandler.messageLog = @"didReceiveNotification FetchCompletion Handler";
    
        //NSUserDefaults* preferences = [NSUserDefaults standardUserDefaults];
        //NSString* notificationStorageKey = @"notifications";
        //[preferences setObject:userInfo forKey:notificationStorageKey];
    
    if (appState == UIApplicationStateActive) {
        pushHandler.notificationMessage = userInfo;
        pushHandler.isInline = YES;
        [pushHandler notificationReceived : userInfo];
    } else {
            //save it for later
            //self.launchNotification = userInfo;
        [pushHandler.pendingNotifications addObject:userInfo];
    }
}*/

- (void)applicationDidBecomeActive:(UIApplication *)application {
    
    NSLog(@"active");
    
    //zero badge
    application.applicationIconBadgeNumber = 0;

    PushPlugin *pushHandler = [self getCommandInstance:@"PushPlugin"];
    if ([pushHandler.pendingNotifications count] > 0) {
        
        pushHandler.notificationMessage = self.launchNotification;
        self.launchNotification = nil;
        [pushHandler performSelectorOnMainThread:@selector(notificationReceived) withObject:pushHandler waitUntilDone:NO];
    }
}

// The accessors use an Associative Reference since you can't define a iVar in a category
// http://developer.apple.com/library/ios/#documentation/cocoa/conceptual/objectivec/Chapters/ocAssociativeReferences.html
- (NSMutableArray *)launchNotification {
   return objc_getAssociatedObject(self, &launchNotificationKey);
}

- (void)setLaunchNotification:(NSDictionary *)aDictionary {
    objc_setAssociatedObject(self, &launchNotificationKey, aDictionary, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)dealloc {
    self.launchNotification = nil; // clear the association and release the object
}

@end