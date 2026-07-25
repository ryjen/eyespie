import Foundation
import eyespie

struct IosModelAssetDownloadConfiguration: Equatable {
    let sourceURL: URL
    let taskDescription: String
    let stagingFilename: String
    let expectedBytes: Int64
    let expectedSHA256: String

    /// Immutable, repository-controlled fixture. This is intentionally separate from future
    /// production model configuration and is small enough for local lifecycle validation.
    static let deterministicTestAsset = IosModelAssetDownloadConfiguration(
        sourceURL: URL(
            string: "https://raw.githubusercontent.com/ryjen/eyespie/745fc97a149fcd67f4a673fd5612f972ec874126/docs/adr/0004-ios-model-asset-delivery.md"
        )!,
        taskDescription: "eyespie-model-test-745fc97a",
        stagingFilename: "ios-model-delivery-test.staged",
        expectedBytes: 5_592,
        expectedSHA256: "245a5c650f640511cbcb62c22507693ea3429db714d6503371b91cb91191d012"
    )
}

protocol ModelAssetDownloadTask: AnyObject {
    var taskIdentifier: Int { get }
    var taskDescription: String? { get set }
    var countOfBytesReceived: Int64 { get }
    var countOfBytesExpectedToReceive: Int64 { get }
    func resume()
    func cancel()
}

extension URLSessionTask: ModelAssetDownloadTask {}

protocol ModelAssetURLSession: AnyObject {
    func makeDownloadTask(with url: URL) -> ModelAssetDownloadTask
    func getAllTasks(completionHandler: @escaping ([URLSessionTask]) -> Void)
}

extension URLSession: ModelAssetURLSession {
    func makeDownloadTask(with url: URL) -> ModelAssetDownloadTask {
        downloadTask(with: url)
    }
}

final class IosModelAssetStagingStore {
    enum StoreError: Error {
        case applicationSupportUnavailable
        case unsafeDestination
        case invalidFileSize
    }

    private let fileManager: FileManager
    private let rootDirectory: URL
    private let stagingFilename: String

    init(
        fileManager: FileManager = .default,
        rootDirectory: URL? = nil,
        stagingFilename: String
    ) throws {
        self.fileManager = fileManager
        self.stagingFilename = stagingFilename

        guard stagingFilename == URL(fileURLWithPath: stagingFilename).lastPathComponent,
              !stagingFilename.contains(".."),
              !stagingFilename.contains("/") else {
            throw StoreError.unsafeDestination
        }

        if let rootDirectory {
            self.rootDirectory = rootDirectory
        } else {
            guard let applicationSupport = fileManager.urls(
                for: .applicationSupportDirectory,
                in: .userDomainMask
            ).first else {
                throw StoreError.applicationSupportUnavailable
            }
            self.rootDirectory = applicationSupport
                .appendingPathComponent("ModelAssets", isDirectory: true)
                .appendingPathComponent("Staging", isDirectory: true)
        }
    }

    var stagedArtifactURL: URL {
        rootDirectory.appendingPathComponent(stagingFilename, isDirectory: false)
    }

    func stageDownloadedFile(at temporaryURL: URL) throws -> (url: URL, bytes: Int64) {
        try fileManager.createDirectory(
            at: rootDirectory,
            withIntermediateDirectories: true,
            attributes: nil
        )

        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        var mutableRoot = rootDirectory
        try mutableRoot.setResourceValues(resourceValues)

        let incomingURL = rootDirectory.appendingPathComponent(
            ".incoming-\(UUID().uuidString)",
            isDirectory: false
        )
        let destinationURL = stagedArtifactURL

        do {
            try fileManager.moveItem(at: temporaryURL, to: incomingURL)
            if fileManager.fileExists(atPath: destinationURL.path) {
                _ = try fileManager.replaceItemAt(
                    destinationURL,
                    withItemAt: incomingURL,
                    backupItemName: nil,
                    options: [.usingNewMetadataOnly]
                )
            } else {
                try fileManager.moveItem(at: incomingURL, to: destinationURL)
            }
        } catch {
            try? fileManager.removeItem(at: incomingURL)
            throw error
        }

        let attributes = try fileManager.attributesOfItem(atPath: destinationURL.path)
        guard let fileSize = attributes[.size] as? NSNumber else {
            throw StoreError.invalidFileSize
        }
        return (destinationURL, fileSize.int64Value)
    }

    func cleanup() throws {
        guard fileManager.fileExists(atPath: rootDirectory.path) else { return }
        try fileManager.removeItem(at: rootDirectory)
    }
}

final class IosUrlSessionModelAssetTransport: NSObject, IosModelAssetTransport {
    typealias SessionFactory = (
        URLSessionConfiguration,
        URLSessionDelegate,
        OperationQueue
    ) -> ModelAssetURLSession

    static func sessionIdentifier(bundleIdentifier: String) -> String {
        "\(bundleIdentifier).model-asset.background-url-session"
    }

    static func makeSessionConfiguration(identifier: String) -> URLSessionConfiguration {
        let configuration = URLSessionConfiguration.background(withIdentifier: identifier)

        // Downloads are explicitly user-initiated, so discretionary scheduling is disabled.
        // The future production artifact is about 584 MB, so cellular, constrained, and
        // expensive-network access are deliberately disabled.
        configuration.isDiscretionary = false
        configuration.allowsCellularAccess = false
        configuration.allowsConstrainedNetworkAccess = false
        configuration.allowsExpensiveNetworkAccess = false
        configuration.waitsForConnectivity = true
        configuration.sessionSendsLaunchEvents = true
        configuration.httpMaximumConnectionsPerHost = 1
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        return configuration
    }

    let backgroundSessionIdentifier: String

    private let configuration: IosModelAssetDownloadConfiguration
    private let eventStream = IosModelAssetTransportEventStream()
    private let stagingStore: IosModelAssetStagingStore
    private let stateQueue = DispatchQueue(label: "com.micrantha.eyespie.model-asset-transport")
    private let delegateQueue: OperationQueue
    private var session: ModelAssetURLSession!
    private var activeTask: ModelAssetDownloadTask?
    private var terminalTaskIdentifiers = Set<Int>()
    private var explicitlyCancelledTaskIdentifiers = Set<Int>()
    private var backgroundCompletionHandler: (() -> Void)?

    init(
        bundleIdentifier: String,
        configuration: IosModelAssetDownloadConfiguration = .deterministicTestAsset,
        stagingStore: IosModelAssetStagingStore? = nil,
        sessionFactory: SessionFactory? = nil
    ) throws {
        self.configuration = configuration
        self.backgroundSessionIdentifier = Self.sessionIdentifier(
            bundleIdentifier: bundleIdentifier
        )
        self.delegateQueue = OperationQueue()
        self.delegateQueue.name = "com.micrantha.eyespie.model-asset-url-session"
        self.delegateQueue.maxConcurrentOperationCount = 1
        if let stagingStore {
            self.stagingStore = stagingStore
        } else {
            self.stagingStore = try IosModelAssetStagingStore(
                stagingFilename: configuration.stagingFilename
            )
        }
        super.init()

        let sessionConfiguration = Self.makeSessionConfiguration(
            identifier: backgroundSessionIdentifier
        )
        let factory = sessionFactory ?? { configuration, delegate, delegateQueue in
            URLSession(
                configuration: configuration,
                delegate: delegate,
                delegateQueue: delegateQueue
            )
        }
        self.session = factory(sessionConfiguration, self, delegateQueue)
        restoreExistingBackgroundTask()
    }

    func observe() -> Kotlinx_coroutines_coreFlow {
        eventStream.observe()
    }

    func schedule(completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        stateQueue.async {
            guard self.configuration.sourceURL.scheme?.lowercased() == "https" else {
                self.eventStream.emitFailed(
                    recoverable: false,
                    diagnosticCode: "model_download_invalid_https_configuration"
                )
                completionHandler(KotlinUnit(), nil)
                return
            }

            if let activeTask = self.activeTask {
                self.emitRestoredState(for: activeTask)
                completionHandler(KotlinUnit(), nil)
                return
            }

            let task = self.session.makeDownloadTask(with: self.configuration.sourceURL)
            task.taskDescription = self.configuration.taskDescription
            self.activeTask = task
            self.eventStream.emitQueued()
            task.resume()
            completionHandler(KotlinUnit(), nil)
        }
    }

    func cancel(completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        stateQueue.async {
            guard let task = self.activeTask else {
                completionHandler(KotlinUnit(), nil)
                return
            }

            self.explicitlyCancelledTaskIdentifiers.insert(task.taskIdentifier)
            self.terminalTaskIdentifiers.insert(task.taskIdentifier)
            self.activeTask = nil
            task.cancel()
            self.eventStream.emitCancelled()
            completionHandler(KotlinUnit(), nil)
        }
    }

    func removeTemporaryArtifacts(completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        stateQueue.async {
            do {
                try self.stagingStore.cleanup()
            } catch {
                self.eventStream.emitFailed(
                    recoverable: false,
                    diagnosticCode: "model_download_staging_cleanup_failed"
                )
            }
            completionHandler(KotlinUnit(), nil)
        }
    }

    func handleEventsForBackgroundSession(
        identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        guard identifier == backgroundSessionIdentifier else {
            completionHandler()
            return
        }

        stateQueue.async {
            let supersededHandler = self.backgroundCompletionHandler
            self.backgroundCompletionHandler = completionHandler
            if let supersededHandler {
                DispatchQueue.main.async(execute: supersededHandler)
            }
        }
    }

    private func restoreExistingBackgroundTask() {
        session.getAllTasks { [weak self] tasks in
            guard let self else { return }
            self.stateQueue.async {
                let matchingTasks = tasks
                    .filter { $0.taskDescription == self.configuration.taskDescription }
                    .sorted { $0.taskIdentifier < $1.taskIdentifier }

                if let activeTask = self.activeTask {
                    matchingTasks
                        .filter { $0.taskIdentifier != activeTask.taskIdentifier }
                        .forEach { $0.cancel() }
                    return
                }

                guard let restoredTask = matchingTasks.last else {
                    self.eventStream.emitIdle()
                    return
                }

                matchingTasks.dropLast().forEach { $0.cancel() }
                self.activeTask = restoredTask
                self.emitRestoredState(for: restoredTask)
            }
        }
    }

    private func emitRestoredState(for task: ModelAssetDownloadTask) {
        if task.countOfBytesReceived > 0 {
            emitProgress(
                downloadedBytes: task.countOfBytesReceived,
                totalBytes: task.countOfBytesExpectedToReceive
            )
        } else {
            eventStream.emitQueued()
        }
    }

    private func emitProgress(downloadedBytes: Int64, totalBytes: Int64) {
        if totalBytes > 0 {
            eventStream.emitDownloading(
                downloadedBytes: downloadedBytes,
                totalBytes: totalBytes
            )
        } else {
            eventStream.emitDownloadingUnknownTotal(downloadedBytes: downloadedBytes)
        }
    }

    private func isCurrent(taskIdentifier: Int) -> Bool {
        activeTask?.taskIdentifier == taskIdentifier &&
            !terminalTaskIdentifiers.contains(taskIdentifier)
    }

    private func finishCurrentTask(taskIdentifier: Int) {
        terminalTaskIdentifiers.insert(taskIdentifier)
        explicitlyCancelledTaskIdentifiers.remove(taskIdentifier)
        if activeTask?.taskIdentifier == taskIdentifier {
            activeTask = nil
        }
    }

    private func mapFailure(_ error: Error) -> (recoverable: Bool, diagnosticCode: String) {
        let urlError = error as? URLError
        switch urlError?.code {
        case .timedOut, .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed,
             .networkConnectionLost, .notConnectedToInternet, .resourceUnavailable,
             .internationalRoamingOff, .callIsActive, .dataNotAllowed:
            return (true, "model_download_network_unavailable")
        case .cancelled:
            return (true, "model_download_cancelled")
        case .badURL, .unsupportedURL, .userAuthenticationRequired:
            return (false, "model_download_remote_configuration_invalid")
        default:
            return (false, "model_download_transport_failed")
        }
    }
}

extension IosUrlSessionModelAssetTransport: URLSessionDownloadDelegate {
    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        stateQueue.async {
            guard self.isCurrent(taskIdentifier: downloadTask.taskIdentifier) else { return }
            self.emitProgress(
                downloadedBytes: totalBytesWritten,
                totalBytes: totalBytesExpectedToWrite
            )
        }
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        stateQueue.async {
            guard self.isCurrent(taskIdentifier: downloadTask.taskIdentifier) else {
                try? FileManager.default.removeItem(at: location)
                return
            }

            do {
                let staged = try self.stagingStore.stageDownloadedFile(at: location)
                self.finishCurrentTask(taskIdentifier: downloadTask.taskIdentifier)
                self.eventStream.emitDownloaded(
                    temporaryPath: staged.url.path,
                    totalBytes: staged.bytes
                )
            } catch {
                self.finishCurrentTask(taskIdentifier: downloadTask.taskIdentifier)
                self.eventStream.emitFailed(
                    recoverable: false,
                    diagnosticCode: "model_download_staging_failed"
                )
            }
        }
    }
}

extension IosUrlSessionModelAssetTransport: URLSessionTaskDelegate {
    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        stateQueue.async {
            let taskIdentifier = task.taskIdentifier
            guard !self.terminalTaskIdentifiers.contains(taskIdentifier),
                  self.activeTask?.taskIdentifier == taskIdentifier else {
                return
            }

            if let error {
                if (error as? URLError)?.code == .cancelled,
                   self.explicitlyCancelledTaskIdentifiers.contains(taskIdentifier) {
                    self.finishCurrentTask(taskIdentifier: taskIdentifier)
                    self.eventStream.emitCancelled()
                    return
                }

                let failure = self.mapFailure(error)
                self.finishCurrentTask(taskIdentifier: taskIdentifier)
                self.eventStream.emitFailed(
                    recoverable: failure.recoverable,
                    diagnosticCode: failure.diagnosticCode
                )
                return
            }

            // A successful download task must first deliver didFinishDownloadingTo. Reaching this
            // callback without a staged artifact is a terminal lifecycle violation.
            self.finishCurrentTask(taskIdentifier: taskIdentifier)
            self.eventStream.emitFailed(
                recoverable: false,
                diagnosticCode: "model_download_missing_staged_artifact"
            )
        }
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        stateQueue.async {
            let completionHandler = self.backgroundCompletionHandler
            self.backgroundCompletionHandler = nil
            if let completionHandler {
                DispatchQueue.main.async(execute: completionHandler)
            }
        }
    }
}
