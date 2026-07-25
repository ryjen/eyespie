import SwiftUI
import UIKit

final class iOSAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        let bundleIdentifier = Bundle.main.bundleIdentifier ?? "com.micrantha.eyespie.ios"
        guard IosUrlSessionModelAssetTransport.retainBackgroundSessionCompletion(
            identifier: identifier,
            bundleIdentifier: bundleIdentifier,
            completionHandler: completionHandler
        ) else {
            return
        }

        // Retain the system handler before constructing the background URLSession. Recreating the
        // session may synchronously begin delegate delivery during application relaunch.
        guard AppComposition.shared.modelAssetTransport != nil else {
            IosUrlSessionModelAssetTransport.completeBackgroundSessionEvents(
                identifier: identifier
            )
            return
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(iOSAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
