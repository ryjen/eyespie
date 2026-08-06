import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

@main
enum IosModelAssetTransportCoreTests {
    static func main() throws {
        try testDeterministicFixtureConfiguration()
        #if canImport(Darwin)
        testSessionConfiguration()
        #endif
        testSingleActiveTaskAndStaleCallbacks()
        testRestorationPlanning()
        testRestoredTaskResumePolicy()
        testTaskInventory()
        testCancellation()
        testProgressMapping()
        testFailureMapping()
        try testStagingAndCleanup()
        testBackgroundCompletionBroker()
        print("IosModelAssetTransportCoreTests: passed")
    }

    private static func require(
        _ condition: @autoclosure () -> Bool,
        _ message: String
    ) {
        guard condition() else {
            fatalError(message)
        }
    }

    private static func testDeterministicFixtureConfiguration() throws {
        let configuration = IosModelAssetDownloadConfiguration.deterministicTestAsset
        require(configuration.sourceURL.scheme == "https", "fixture must use HTTPS")
        require(configuration.sourceURL.user == nil, "fixture must not contain credentials")
        require(configuration.sourceURL.query == nil, "fixture must not contain query secrets")
        require(configuration.expectedBytes == 5_592, "fixture byte count changed")
        require(
            configuration.expectedSHA256 ==
                "245a5c650f640511cbcb62c22507693ea3429db714d6503371b91cb91191d012",
            "fixture digest changed"
        )
    }

    #if canImport(Darwin)
    private static func testSessionConfiguration() {
        let identifier = "com.micrantha.eyespie.tests.model-asset.background-url-session"
        let configuration = IosModelAssetSessionPolicy.makeConfiguration(
            identifier: identifier
        )
        require(configuration.identifier == identifier, "session identifier must be stable")
        require(!configuration.isDiscretionary, "user-initiated download must not be discretionary")
        require(!configuration.allowsCellularAccess, "cellular access must be disabled")
        require(!configuration.allowsConstrainedNetworkAccess, "constrained access must be disabled")
        require(!configuration.allowsExpensiveNetworkAccess, "expensive access must be disabled")
        require(configuration.waitsForConnectivity, "connectivity waiting must be enabled")
        require(configuration.sessionSendsLaunchEvents, "launch events must be enabled")
        require(configuration.httpMaximumConnectionsPerHost == 1, "only one connection is allowed")
    }
    #endif

    private static func testSingleActiveTaskAndStaleCallbacks() {
        var state = IosModelAssetTaskStateMachine()
        require(state.schedule(taskIdentifier: 10), "first task must schedule")
        require(!state.schedule(taskIdentifier: 11), "second task must be rejected")
        require(state.acceptsCallback(taskIdentifier: 10), "active callback must be accepted")
        require(!state.acceptsCallback(taskIdentifier: 9), "stale callback must be rejected")
        require(state.finish(taskIdentifier: 10), "active task must finish once")
        require(!state.finish(taskIdentifier: 10), "duplicate completion must be idempotent")
        require(state.schedule(taskIdentifier: 11), "new task must schedule after completion")
        require(!state.acceptsCallback(taskIdentifier: 10), "old callback cannot overwrite new task")
        require(state.acceptsCallback(taskIdentifier: 11), "new callback must be accepted")
    }

    private static func testRestorationPlanning() {
        var state = IosModelAssetTaskStateMachine()
        let initial = state.restorationPlan(candidateTaskIdentifiers: [3, 1, 2, 3])
        require(initial.restoredTaskIdentifier == 3, "newest restored task must win")
        require(initial.cancelledTaskIdentifiers == [1, 2], "older restored tasks must cancel")
        require(state.acceptsCallback(taskIdentifier: 3), "restored task must become active")

        let whileActive = state.restorationPlan(candidateTaskIdentifiers: [2, 3, 4])
        require(whileActive.restoredTaskIdentifier == nil, "active task must not be replaced")
        require(whileActive.cancelledTaskIdentifiers == [2, 4], "duplicates must cancel")
    }

    private static func testRestoredTaskResumePolicy() {
        let suspended = FakeModelAssetDownloadTask(
            taskIdentifier: 7,
            taskDescription: "expected",
            isSuspended: true
        )

        IosModelAssetTaskRestorationPolicy.resumeIfSuspended(suspended)
        IosModelAssetTaskRestorationPolicy.resumeIfSuspended(suspended)

        require(suspended.resumeCalls == 1, "suspended restored task must resume once")
        require(!suspended.isSuspended, "resumed task must no longer be suspended")

        let running = FakeModelAssetDownloadTask(
            taskIdentifier: 8,
            taskDescription: "expected",
            isSuspended: false
        )

        IosModelAssetTaskRestorationPolicy.resumeIfSuspended(running)

        require(running.resumeCalls == 0, "running restored task must not be resumed")
    }

    private static func testTaskInventory() {
        let matching = FakeModelAssetDownloadTask(
            taskIdentifier: 1,
            taskDescription: "expected"
        )
        let stale = FakeModelAssetDownloadTask(
            taskIdentifier: 2,
            taskDescription: "old-configuration"
        )
        let missingDescription = FakeModelAssetDownloadTask(
            taskIdentifier: 3,
            taskDescription: nil
        )

        let inventory = IosModelAssetTaskInventory.partition(
            tasks: [stale, matching, missingDescription],
            expectedTaskDescription: "expected"
        )

        require(
            inventory.matchingTasks.map(\.taskIdentifier) == [1],
            "only tasks for the current configuration may restore"
        )
        require(
            Set(inventory.staleTasks.map(\.taskIdentifier)) == Set([2, 3]),
            "tasks from older configurations must be stale"
        )

        inventory.staleTasks.forEach { $0.cancel() }
        require(!matching.wasCancelled, "current task must not be cancelled")
        require(stale.wasCancelled, "old configuration task must be cancelled")
        require(missingDescription.wasCancelled, "unidentified session task must be cancelled")
    }

    private static func testCancellation() {
        var state = IosModelAssetTaskStateMachine()
        require(state.schedule(taskIdentifier: 5), "task must schedule")
        require(state.cancelCurrentTask() == 5, "active task must cancel")
        require(!state.acceptsCallback(taskIdentifier: 5), "cancelled callback must be ignored")
        require(state.cancelCurrentTask() == nil, "duplicate cancellation must be idempotent")
    }

    private static func testProgressMapping() {
        require(
            IosModelAssetProgress.map(downloadedBytes: 0, totalBytes: 100) == .queued,
            "zero-byte restored tasks must remain queued"
        )
        require(
            IosModelAssetProgress.map(downloadedBytes: 12, totalBytes: 100) ==
                .known(downloadedBytes: 12, totalBytes: 100),
            "known totals must remain known"
        )
        require(
            IosModelAssetProgress.map(downloadedBytes: 12, totalBytes: -1) ==
                .unknown(downloadedBytes: 12),
            "unknown totals must remain unknown"
        )
    }

    private static func testFailureMapping() {
        require(
            IosModelAssetFailurePolicy.map(error: URLError(.notConnectedToInternet)) ==
                IosModelAssetFailure(
                    recoverable: true,
                    diagnosticCode: "model_download_network_unavailable"
                ),
            "network outage must be recoverable"
        )
        require(
            IosModelAssetFailurePolicy.map(error: URLError(.badURL)) ==
                IosModelAssetFailure(
                    recoverable: false,
                    diagnosticCode: "model_download_remote_configuration_invalid"
                ),
            "bad URL must be terminal"
        )
        require(
            IosModelAssetFailurePolicy.mapHTTP(statusCode: 503).recoverable,
            "server outage must be recoverable"
        )
        require(
            !IosModelAssetFailurePolicy.mapHTTP(statusCode: 404).recoverable,
            "missing immutable artifact must be terminal"
        )
    }

    private static func testStagingAndCleanup() throws {
        let fileManager = FileManager.default
        let root = fileManager.temporaryDirectory.appendingPathComponent(
            "eyespie-native-tests-\(UUID().uuidString)",
            isDirectory: true
        )
        defer { try? fileManager.removeItem(at: root) }

        let store = try IosModelAssetStagingStore(
            rootDirectory: root.appendingPathComponent("staging", isDirectory: true),
            stagingFilename: "model.staged"
        )
        let first = root.appendingPathComponent("system-first.tmp")
        try fileManager.createDirectory(at: root, withIntermediateDirectories: true)
        try Data("fixture".utf8).write(to: first)

        let staged = try store.stageDownloadedFile(at: first)
        require(staged.url.lastPathComponent == "model.staged", "server filename must be ignored")
        require(staged.bytes == 7, "staged byte count must be preserved")
        require(!fileManager.fileExists(atPath: first.path), "system temporary file must move")

        let replacement = root.appendingPathComponent("untrusted-name.bin")
        try Data("replacement".utf8).write(to: replacement)
        _ = try store.stageDownloadedFile(at: replacement)
        let replacementData = try Data(contentsOf: store.stagedArtifactURL)
        require(
            replacementData == Data("replacement".utf8),
            "staging replacement must be atomic and deterministic"
        )

        try store.cleanup()
        require(
            !fileManager.fileExists(atPath: store.stagedArtifactURL.deletingLastPathComponent().path),
            "cleanup must remove app-owned staging"
        )

        for unsafeFilename in ["../escape", "..\\escape"] {
            do {
                _ = try IosModelAssetStagingStore(
                    rootDirectory: root,
                    stagingFilename: unsafeFilename
                )
                fatalError("path traversal must be rejected")
            } catch IosModelAssetStagingStore.StoreError.unsafeDestination {
                // Expected.
            } catch {
                fatalError("unexpected traversal error: \(error)")
            }
        }
    }

    private static func testBackgroundCompletionBroker() {
        let broker = IosBackgroundSessionCompletionBroker.shared
        let identifier = "com.micrantha.eyespie.tests.\(UUID().uuidString)"
        var completions = 0

        require(
            !broker.retain(
                identifier: "other-session",
                expectedIdentifier: identifier,
                completionHandler: { completions += 1 }
            ),
            "unrelated sessions must not be retained"
        )
        require(completions == 1, "unrelated completion must run immediately")

        require(
            broker.retain(
                identifier: identifier,
                expectedIdentifier: identifier,
                completionHandler: { completions += 1 }
            ),
            "matching session must be retained"
        )
        require(
            broker.retain(
                identifier: identifier,
                expectedIdentifier: identifier,
                completionHandler: { completions += 1 }
            ),
            "duplicate matching handler must be retained"
        )
        require(completions == 1, "retained handlers must wait for delegate drain")

        broker.drain(identifier: identifier).forEach { $0() }
        require(completions == 3, "all retained handlers must run after drain")
        require(broker.drain(identifier: identifier).isEmpty, "duplicate drain must be idempotent")
    }
}

private final class FakeModelAssetDownloadTask: ModelAssetDownloadTask {
    let taskIdentifier: Int
    var taskDescription: String?
    let countOfBytesReceived: Int64 = 0
    let countOfBytesExpectedToReceive: Int64 = -1
    var isSuspended: Bool
    private(set) var resumeCalls = 0
    private(set) var wasCancelled = false

    init(
        taskIdentifier: Int,
        taskDescription: String?,
        isSuspended: Bool = false
    ) {
        self.taskIdentifier = taskIdentifier
        self.taskDescription = taskDescription
        self.isSuspended = isSuspended
    }

    func resume() {
        resumeCalls += 1
        isSuspended = false
    }

    func cancel() {
        wasCancelled = true
    }
}
