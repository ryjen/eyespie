import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(iOSAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
