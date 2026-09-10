import Foundation
import SwiftUI
import eyespie

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
#if DEBUG
    @State private var didStartEmbeddingCalibration = false
#endif

    var body: some View {
#if DEBUG
        ComposeView().ignoresSafeArea(.all, edges: .bottom)
            .onOpenURL(perform: handleExternalURL)
            .onAppear {
                runEmbeddingCalibrationIfRequested()
            }
#else
        ComposeView().ignoresSafeArea(.all, edges: .bottom)
            .onOpenURL(perform: handleExternalURL)
#endif
    }

    private func handleExternalURL(_ url: URL) {
        if url.isFileURL {
            _ = IosExternalIngressKt.offerIosExternalDocument(url: url as NSURL)
            return
        }

        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return
        }
        let pathSegments = url.pathComponents.filter { component in
            component != "/" && !component.isEmpty
        }
        _ = IosExternalIngressKt.offerIosDeepLink(
            scheme: components.scheme,
            host: components.host,
            pathSegments: pathSegments,
            hasQuery: components.query != nil,
            hasFragment: components.fragment != nil,
            hasUserInfo: components.user != nil || components.password != nil,
            hasPort: components.port != nil
        )
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
                let report = try IosImageEmbeddingCalibrationCollector().collect()
                let documents = try FileManager.default.url(
                    for: .documentDirectory,
                    in: .userDomainMask,
                    appropriateFor: nil,
                    create: true
                )
                let output = documents.appendingPathComponent("image-embedding-calibration-ios.json")
                try report.write(to: output, atomically: true, encoding: .utf8)
                print("EYESPIE_IMAGE_EMBEDDING_CALIBRATION=complete")
            } catch {
                print("EYESPIE_IMAGE_EMBEDDING_CALIBRATION=failed")
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
