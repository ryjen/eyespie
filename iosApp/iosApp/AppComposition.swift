import Foundation
import eyespie

final class AppComposition {
    static let shared = AppComposition()

    let modelAssetTransport: IosUrlSessionModelAssetTransport
    let kotlinAppDelegate: eyespie.AppDelegate

    private init() {
        let bundleIdentifier = Bundle.main.bundleIdentifier ?? "com.micrantha.eyespie.ios"
        let transport = IosUrlSessionModelAssetTransport(
            bundleIdentifier: bundleIdentifier,
            configuration: .deterministicTestAsset
        )
        let appDelegate = eyespie.AppDelegate(
            networkMonitor: iOSNetworkMonitor(),
            packageId: "com.micrantha.eyespie"
        )
        appDelegate.installModelAssetTransport(transport: transport)

        self.modelAssetTransport = transport
        self.kotlinAppDelegate = appDelegate
    }
}
