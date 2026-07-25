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

        // Retain before reconstructing the session: restoration may immediately deliver events.
        guard AppComposition.shared.modelAssetTransport != nil else {
            IosUrlSessionModelAssetTransport.completeBackgroundSessionEvents(
                identifier: identifier
            )
            return
        }
    }
}
