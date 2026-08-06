import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
#if canImport(Network)
import Network
#endif
#if canImport(eyespie)
import eyespie
#endif

#if canImport(eyespie)
final class iOSNetworkMonitor: NetworkMonitor {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")

    func startMonitoring(onUpdate: @escaping (KotlinBoolean) -> Void) {
        monitor.pathUpdateHandler = { path in
            onUpdate(KotlinBoolean(value: path.status == .satisfied))
        }
        monitor.start(queue: queue)
    }

    func stopMonitoring() {
        monitor.cancel()
    }
}
#endif

// MARK: - Model asset configuration

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

#if canImport(Darwin)
enum IosModelAssetSessionPolicy {
    static func makeConfiguration(identifier: String) -> URLSessionConfiguration {
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
}
#endif

// MARK: - Deterministic coordinator state

struct IosModelAssetRestorationPlan: Equatable {
    let restoredTaskIdentifier: Int?
    let cancelledTaskIdentifiers: [Int]
}

struct IosModelAssetTaskStateMachine {
    private(set) var activeTaskIdentifier: Int?
    private var terminalTaskIdentifiers = Set<Int>()

    mutating func schedule(taskIdentifier: Int) -> Bool {
        guard activeTaskIdentifier == nil else { return false }
        terminalTaskIdentifiers.remove(taskIdentifier)
        activeTaskIdentifier = taskIdentifier
        return true
    }

    mutating func restorationPlan(
        candidateTaskIdentifiers: [Int]
    ) -> IosModelAssetRestorationPlan {
        let candidates = Array(Set(candidateTaskIdentifiers)).sorted()

        if let activeTaskIdentifier {
            return IosModelAssetRestorationPlan(
                restoredTaskIdentifier: nil,
                cancelledTaskIdentifiers: candidates.filter { $0 != activeTaskIdentifier }
            )
        }

        guard let restoredTaskIdentifier = candidates.last else {
            return IosModelAssetRestorationPlan(
                restoredTaskIdentifier: nil,
                cancelledTaskIdentifiers: []
            )
        }

        terminalTaskIdentifiers.remove(restoredTaskIdentifier)
        activeTaskIdentifier = restoredTaskIdentifier
        return IosModelAssetRestorationPlan(
            restoredTaskIdentifier: restoredTaskIdentifier,
            cancelledTaskIdentifiers: Array(candidates.dropLast())
        )
    }

    func acceptsCallback(taskIdentifier: Int) -> Bool {
        activeTaskIdentifier == taskIdentifier &&
            !terminalTaskIdentifiers.contains(taskIdentifier)
    }

    @discardableResult
    mutating func finish(taskIdentifier: Int) -> Bool {
        guard acceptsCallback(taskIdentifier: taskIdentifier) else { return false }
        terminalTaskIdentifiers.insert(taskIdentifier)
        activeTaskIdentifier = nil
        return true
    }

    mutating func cancelCurrentTask() -> Int? {
        guard let activeTaskIdentifier else { return nil }
        terminalTaskIdentifiers.insert(activeTaskIdentifier)
        self.activeTaskIdentifier = nil
        return activeTaskIdentifier
    }
}

enum IosModelAssetProgress: Equatable {
    case queued
    case known(downloadedBytes: Int64, totalBytes: Int64)
    case unknown(downloadedBytes: Int64)

    static func map(downloadedBytes: Int64, totalBytes: Int64) -> Self {
        guard downloadedBytes > 0 else { return .queued }
        if totalBytes > 0 {
            return .known(downloadedBytes: downloadedBytes, totalBytes: totalBytes)
        }
        return .unknown(downloadedBytes: downloadedBytes)
    }
}

struct IosModelAssetFailure: Equatable {
    let recoverable: Bool
    let diagnosticCode: String
}

enum IosModelAssetFailurePolicy {
    static func map(error: Error) -> IosModelAssetFailure {
        let urlError = error as? URLError
        switch urlError?.code {
        case .timedOut, .cannotFindHost, .cannotConnectToHost, .dnsLookupFailed,
             .networkConnectionLost, .notConnectedToInternet, .resourceUnavailable,
             .internationalRoamingOff, .callIsActive, .dataNotAllowed:
            return IosModelAssetFailure(
                recoverable: true,
                diagnosticCode: "model_download_network_unavailable"
            )
        case .cancelled:
            return IosModelAssetFailure(
                recoverable: true,
                diagnosticCode: "model_download_cancelled"
            )
        case .badURL, .unsupportedURL, .userAuthenticationRequired:
            return IosModelAssetFailure(
                recoverable: false,
                diagnosticCode: "model_download_remote_configuration_invalid"
            )
        default:
            return IosModelAssetFailure(
                recoverable: false,
                diagnosticCode: "model_download_transport_failed"
            )
        }
    }

    static func mapHTTP(statusCode: Int) -> IosModelAssetFailure {
        switch statusCode {
        case 408, 425, 429, 500 ... 599:
            return IosModelAssetFailure(
                recoverable: true,
                diagnosticCode: "model_download_http_temporarily_unavailable"
            )
        default:
            return IosModelAssetFailure(
                recoverable: false,
                diagnosticCode: "model_download_http_response_invalid"
            )
        }
    }
}

final class IosBackgroundSessionCompletionBroker: @unchecked Sendable {
    static let shared = IosBackgroundSessionCompletionBroker()

    private let queue = DispatchQueue(
        label: "com.micrantha.eyespie.model-asset-background-handlers"
    )
    private var handlersByIdentifier: [String: [() -> Void]] = [:]

    private init() {}

    @discardableResult
    func retain(
        identifier: String,
        expectedIdentifier: String,
        completionHandler: @escaping () -> Void
    ) -> Bool {
        guard identifier == expectedIdentifier else {
            completionHandler()
            return false
        }

        queue.sync {
            handlersByIdentifier[identifier, default: []].append(completionHandler)
        }
        return true
    }

    func drain(identifier: String) -> [() -> Void] {
        queue.sync {
            handlersByIdentifier.removeValue(forKey: identifier) ?? []
        }
    }
}

// MARK: - URLSession seams

protocol ModelAssetDownloadTask: AnyObject {
    var taskIdentifier: Int { get }
    var taskDescription: String? { get set }
    var countOfBytesReceived: Int64 { get }
    var countOfBytesExpectedToReceive: Int64 { get }
    var isSuspended: Bool { get }
    func resume()
    func cancel()
}

extension URLSessionTask: ModelAssetDownloadTask {
    var isSuspended: Bool {
        state == .suspended
    }
}

enum IosModelAssetTaskRestorationPolicy {
    static func resumeIfSuspended(_ task: ModelAssetDownloadTask) {
        guard task.isSuspended else { return }
        task.resume()
    }
}

protocol ModelAssetURLSession: AnyObject {
    func makeDownloadTask(with url: URL) -> ModelAssetDownloadTask
    func existingTasks(completionHandler: @escaping ([ModelAssetDownloadTask]) -> Void)
}

extension URLSession: ModelAssetURLSession {
    func makeDownloadTask(with url: URL) -> ModelAssetDownloadTask {
        downloadTask(with: url)
    }

    func existingTasks(completionHandler: @escaping ([ModelAssetDownloadTask]) -> Void) {
        getAllTasks { tasks in
            completionHandler(tasks.map { $0 as ModelAssetDownloadTask })
        }
    }
}

struct IosModelAssetTaskInventory {
    let matchingTasks: [ModelAssetDownloadTask]
    let staleTasks: [ModelAssetDownloadTask]

    static func partition(
        tasks: [ModelAssetDownloadTask],
        expectedTaskDescription: String
    ) -> IosModelAssetTaskInventory {
        IosModelAssetTaskInventory(
            matchingTasks: tasks.filter {
                $0.taskDescription == expectedTaskDescription
            },
            staleTasks: tasks.filter {
                $0.taskDescription != expectedTaskDescription
            }
        )
    }
}

// MARK: - App-owned staging

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
              !stagingFilename.contains("/"),
              !stagingFilename.contains("\\") else {
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

        #if canImport(Darwin)
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        var mutableRoot = rootDirectory
        try mutableRoot.setResourceValues(resourceValues)
        #endif

        let incomingURL = rootDirectory.appendingPathComponent(
            ".incoming-\(UUID().uuidString)",
            isDirectory: false
        )
        let destinationURL = stagedArtifactURL

        do {
            try fileManager.moveItem(at: temporaryURL, to: incomingURL)
            if fileManager.fileExists(atPath: destinationURL.path) {
                #if canImport(Darwin)
                _ = try fileManager.replaceItemAt(
                    destinationURL,
                    withItemAt: incomingURL,
                    backupItemName: nil,
                    options: [.usingNewMetadataOnly]
                )
                #else
                try fileManager.removeItem(at: destinationURL)
                try fileManager.moveItem(at: incomingURL, to: destinationURL)
                #endif
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

#if canImport(eyespie)
// MARK: - Native background transport

final class IosUrlSessionModelAssetTransport: NSObject, IosModelAssetTransport {
    typealias SessionFactory = (
        URLSessionConfiguration,
        URLSessionDelegate,
        OperationQueue
    ) -> ModelAssetURLSession

    static func sessionIdentifier(bundleIdentifier: String) -> String {
        "\(bundleIdentifier).model-asset.background-url-session"
    }

    @discardableResult
    static func retainBackgroundSessionCompletion(
        identifier: String,
        bundleIdentifier: String,
        completionHandler: @escaping () -> Void
    ) -> Bool {
        IosBackgroundSessionCompletionBroker.shared.retain(
            identifier: identifier,
            expectedIdentifier: sessionIdentifier(bundleIdentifier: bundleIdentifier),
            completionHandler: completionHandler
        )
    }

    static func completeBackgroundSessionEvents(identifier: String) {
        let completionHandlers = IosBackgroundSessionCompletionBroker.shared.drain(
            identifier: identifier
        )
        guard !completionHandlers.isEmpty else { return }

        DispatchQueue.main.async {
            completionHandlers.forEach { $0() }
        }
    }

    static func makeSessionConfiguration(identifier: String) -> URLSessionConfiguration {
        IosModelAssetSessionPolicy.makeConfiguration(identifier: identifier)
    }

    let backgroundSessionIdentifier: String

    private let configuration: IosModelAssetDownloadConfiguration
    private let eventStream = IosModelAssetTransportEventStream()
    private let stagingStore: IosModelAssetStagingStore
    private let stateQueue = DispatchQueue(label: "com.micrantha.eyespie.model-asset-transport")
    private let delegateQueue: OperationQueue
    private var session: ModelAssetURLSession!
    private var activeTask: ModelAssetDownloadTask?
    private var taskState = IosModelAssetTaskStateMachine()

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
            guard self.taskState.schedule(taskIdentifier: task.taskIdentifier) else {
                task.cancel()
                self.eventStream.emitFailed(
                    recoverable: false,
                    diagnosticCode: "model_download_single_task_invariant_failed"
                )
                completionHandler(KotlinUnit(), nil)
                return
            }

            self.activeTask = task
            self.eventStream.emitQueued()
            task.resume()
            completionHandler(KotlinUnit(), nil)
        }
    }

    func cancel(completionHandler: @escaping (KotlinUnit?, Error?) -> Void) {
        stateQueue.async {
            guard let task = self.activeTask,
                  self.taskState.cancelCurrentTask() == task.taskIdentifier else {
                completionHandler(KotlinUnit(), nil)
                return
            }

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

    private func restoreExistingBackgroundTask() {
        session.existingTasks { [weak self] tasks in
            guard let self else { return }
            self.stateQueue.async {
                let inventory = IosModelAssetTaskInventory.partition(
                    tasks: tasks,
                    expectedTaskDescription: self.configuration.taskDescription
                )
                inventory.staleTasks.forEach { $0.cancel() }

                let matchingTasks = inventory.matchingTasks.sorted {
                    $0.taskIdentifier < $1.taskIdentifier
                }
                let plan = self.taskState.restorationPlan(
                    candidateTaskIdentifiers: matchingTasks.map(\.taskIdentifier)
                )

                let tasksByIdentifier = Dictionary(
                    uniqueKeysWithValues: matchingTasks.map { ($0.taskIdentifier, $0) }
                )
                plan.cancelledTaskIdentifiers.forEach {
                    tasksByIdentifier[$0]?.cancel()
                }

                guard let restoredTaskIdentifier = plan.restoredTaskIdentifier,
                      self.activeTask == nil,
                      let restoredTask = tasksByIdentifier[restoredTaskIdentifier] else {
                    if self.taskState.activeTaskIdentifier == nil {
                        self.eventStream.emitIdle()
                    }
                    return
                }

                self.activeTask = restoredTask
                self.emitRestoredState(for: restoredTask)
                IosModelAssetTaskRestorationPolicy.resumeIfSuspended(restoredTask)
            }
        }
    }

    private func emitRestoredState(for task: ModelAssetDownloadTask) {
        emitProgress(
            downloadedBytes: task.countOfBytesReceived,
            totalBytes: task.countOfBytesExpectedToReceive
        )
    }

    private func emitProgress(downloadedBytes: Int64, totalBytes: Int64) {
        switch IosModelAssetProgress.map(
            downloadedBytes: downloadedBytes,
            totalBytes: totalBytes
        ) {
        case .queued:
            eventStream.emitQueued()
        case let .known(downloadedBytes, totalBytes):
            eventStream.emitDownloading(
                downloadedBytes: downloadedBytes,
                totalBytes: totalBytes
            )
        case let .unknown(downloadedBytes):
            eventStream.emitDownloadingUnknownTotal(downloadedBytes: downloadedBytes)
        }
    }

    private func isCurrent(taskIdentifier: Int) -> Bool {
        taskState.acceptsCallback(taskIdentifier: taskIdentifier)
    }

    @discardableResult
    private func finishCurrentTask(taskIdentifier: Int) -> Bool {
        guard taskState.finish(taskIdentifier: taskIdentifier) else { return false }
        if activeTask?.taskIdentifier == taskIdentifier {
            activeTask = nil
        }
        return true
    }

    private func failCurrentTask(
        taskIdentifier: Int,
        failure: IosModelAssetFailure
    ) {
        guard finishCurrentTask(taskIdentifier: taskIdentifier) else { return }
        eventStream.emitFailed(
            recoverable: failure.recoverable,
            diagnosticCode: failure.diagnosticCode
        )
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
            let taskIdentifier = downloadTask.taskIdentifier
            guard self.isCurrent(taskIdentifier: taskIdentifier) else {
                try? FileManager.default.removeItem(at: location)
                return
            }

            guard let response = downloadTask.response as? HTTPURLResponse else {
                try? FileManager.default.removeItem(at: location)
                self.failCurrentTask(
                    taskIdentifier: taskIdentifier,
                    failure: IosModelAssetFailure(
                        recoverable: false,
                        diagnosticCode: "model_download_response_missing"
                    )
                )
                return
            }

            guard (200 ... 299).contains(response.statusCode) else {
                try? FileManager.default.removeItem(at: location)
                self.failCurrentTask(
                    taskIdentifier: taskIdentifier,
                    failure: IosModelAssetFailurePolicy.mapHTTP(
                        statusCode: response.statusCode
                    )
                )
                return
            }

            do {
                let staged = try self.stagingStore.stageDownloadedFile(at: location)
                guard self.finishCurrentTask(taskIdentifier: taskIdentifier) else {
                    try? FileManager.default.removeItem(at: staged.url)
                    return
                }
                self.eventStream.emitDownloaded(
                    temporaryPath: staged.url.path,
                    totalBytes: staged.bytes
                )
            } catch {
                self.failCurrentTask(
                    taskIdentifier: taskIdentifier,
                    failure: IosModelAssetFailure(
                        recoverable: false,
                        diagnosticCode: "model_download_staging_failed"
                    )
                )
            }
        }
    }
}

extension IosUrlSessionModelAssetTransport {
    func urlSession(
        _ session: URLSession,
        taskIsWaitingForConnectivity task: URLSessionTask
    ) {
        stateQueue.async {
            guard self.isCurrent(taskIdentifier: task.taskIdentifier) else { return }
            self.eventStream.emitWaitingForNetwork()
        }
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        stateQueue.async {
            let taskIdentifier = task.taskIdentifier
            guard self.isCurrent(taskIdentifier: taskIdentifier) else { return }

            if let error {
                self.failCurrentTask(
                    taskIdentifier: taskIdentifier,
                    failure: IosModelAssetFailurePolicy.map(error: error)
                )
                return
            }

            // A successful task must first deliver didFinishDownloadingTo. Reaching this callback
            // without a staged artifact is a terminal lifecycle violation.
            self.failCurrentTask(
                taskIdentifier: taskIdentifier,
                failure: IosModelAssetFailure(
                    recoverable: false,
                    diagnosticCode: "model_download_missing_staged_artifact"
                )
            )
        }
    }

    func urlSessionDidFinishEvents(forBackgroundURLSession session: URLSession) {
        stateQueue.async {
            Self.completeBackgroundSessionEvents(identifier: self.backgroundSessionIdentifier)
        }
    }
}

// MARK: - Application composition

final class AppComposition {
    static let shared = AppComposition()

    let modelAssetTransport: IosUrlSessionModelAssetTransport?
    let kotlinAppDelegate: eyespie.AppDelegate

    private init() {
        let appDelegate = eyespie.AppDelegate(
            networkMonitor: iOSNetworkMonitor(),
            packageId: "com.micrantha.eyespie"
        )
        let bundleIdentifier = Bundle.main.bundleIdentifier ?? "com.micrantha.eyespie.ios"
        let transport = try? IosUrlSessionModelAssetTransport(
            bundleIdentifier: bundleIdentifier,
            configuration: .deterministicTestAsset
        )

        if let transport {
            appDelegate.installModelAssetTransport(transport: transport)
        }

        self.modelAssetTransport = transport
        self.kotlinAppDelegate = appDelegate
    }
}
#endif
