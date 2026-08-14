import Foundation
import SwiftUI
import eyespie

enum ConfigError: Error {
    case general(reason: String)
}

struct ComposeView: UIViewControllerRepresentable {
    var application = IOSApplication(component: AppDelegate(
        networkMonitor: iOSNetworkMonitor(),
        packageId: "com.micrantha.eyespie"
    )
    )

    func makeUIViewController(context: Context) -> UIViewController {
        application.createViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        application.update(viewController: uiViewController)
    }

    func dismantleUIViewController(_ uiViewController: Self.UIViewControllerType, coordinator: Self.Coordinator) {
        application.finish(viewController: uiViewController)
    }
}

struct ContentView: View {
#if DEBUG
    @State private var didStartEmbeddingCalibration = false
#endif

    var body: some View {
#if DEBUG
        ComposeView().ignoresSafeArea(.all, edges: .bottom)
            .onAppear {
                runEmbeddingCalibrationIfRequested()
            }
#else
        ComposeView().ignoresSafeArea(.all, edges: .bottom)
#endif
    }

#if DEBUG
    private func runEmbeddingCalibrationIfRequested() {
        guard !didStartEmbeddingCalibration else { return }
        guard ProcessInfo.processInfo.environment["EYESPIE_IMAGE_EMBEDDING_CALIBRATION"] == "1" else {
            return
        }
        didStartEmbeddingCalibration = true

        Task.detached(priority: .userInitiated) {
            do {
                let report = try ImageEmbeddingCalibrationCollector().collect()
                let documents = try FileManager.default.url(
                    for: .documentDirectory,
                    in: .userDomainMask,
                    appropriateFor: nil,
                    create: true
                )
                let output = documents
                    .appendingPathComponent("image-embedding-calibration-ios")
                    .appendingPathExtension("json")
                try report.write(to: output, atomically: true, encoding: .utf8)
                print("EYESPIE_IMAGE_EMBEDDING_CALIBRATION=\(output.path)")
            } catch {
                print("EYESPIE_IMAGE_EMBEDDING_CALIBRATION_ERROR=\(error)")
            }
        }
    }
#endif
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
