import UIKit

final class iOSAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        AppComposition.shared.modelAssetTransport.handleEventsForBackgroundSession(
            identifier: identifier,
            completionHandler: completionHandler
        )
    }
}
