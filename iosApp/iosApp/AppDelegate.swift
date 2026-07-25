import UIKit

final class iOSAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        guard let transport = AppComposition.shared.modelAssetTransport else {
            completionHandler()
            return
        }

        transport.handleEventsForBackgroundSession(
            identifier: identifier,
            completionHandler: completionHandler
        )
    }
}
