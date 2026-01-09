package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.domain.MutableThreadSafeMap
import com.micrantha.bluebell.observability.debug
import com.micrantha.bluebell.observability.domain.CacheFullException
import com.micrantha.bluebell.observability.domain.CacheWriteException
import com.micrantha.bluebell.observability.domain.CampaignAlreadyExistsException
import com.micrantha.bluebell.observability.domain.DestinationException
import com.micrantha.bluebell.observability.domain.DestinationRejectionException
import com.micrantha.bluebell.observability.domain.EventCache
import com.micrantha.bluebell.observability.domain.EventDestination
import com.micrantha.bluebell.observability.domain.EventTooLargeException
import com.micrantha.bluebell.observability.domain.NetworkException
import com.micrantha.bluebell.observability.domain.ObservabilityRepository
import com.micrantha.bluebell.observability.domain.RateLimitException
import com.micrantha.bluebell.observability.domain.RetryQueue
import com.micrantha.bluebell.observability.domain.SessionInfo
import com.micrantha.bluebell.observability.domain.SessionInfoProvider
import com.micrantha.bluebell.observability.domain.SchemaRegistry
import com.micrantha.bluebell.observability.domain.SchemaValidationException
import com.micrantha.bluebell.observability.domain.SpanAlreadyEndedException
import com.micrantha.bluebell.observability.domain.SpanNotFoundException
import com.micrantha.bluebell.observability.domain.getRetryDelay
import com.micrantha.bluebell.observability.domain.isRetryable
import com.micrantha.bluebell.observability.domain.toFailureReason
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.AuditEvent
import com.micrantha.bluebell.observability.entity.BatchResult
import com.micrantha.bluebell.observability.entity.Campaign
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationStatus
import com.micrantha.bluebell.observability.entity.DeviceInfo
import com.micrantha.bluebell.observability.entity.EventFailure
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.EventSpan
import com.micrantha.bluebell.observability.entity.NetworkInfo
import com.micrantha.bluebell.observability.entity.ObservabilityConfig
import com.micrantha.bluebell.observability.entity.RetryableEvent
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.SystemEvent
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.TimeRange
import com.micrantha.bluebell.observability.entity.TraceContext
import com.micrantha.bluebell.observability.entity.ValidationResult
import com.micrantha.bluebell.observability.error
import com.micrantha.bluebell.observability.info
import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.observability.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class ObservabilityDataRepository(
    private val destinations: Map<Destination, EventDestination>,
    private val cache: EventCache,
    private val schemaRegistry: SchemaRegistry,
    private val retryQueue: RetryQueue,
    private val scope: CoroutineScope,
    private val config: ObservabilityConfig = ObservabilityConfig.default(),
    private val deviceInfo: DeviceInfo? = null,
    private val sessionInfoProvider: SessionInfoProvider = SessionInfoProvider { SessionInfo() }
) : ObservabilityRepository {

    // Thread-safe state
    private val globalProperties = MutableThreadSafeMap<String, Any>()
    private val activeSpans = MutableThreadSafeMap<String, EventSpan>()
    private val activeCampaigns = MutableThreadSafeMap<String, Campaign>()

    private val logger by logger()

    init {
        startRetryProcessor()
        scope.launch {
            restoreQueuedEvents()
        }
    }

    override suspend fun record(event: TelemetryEvent): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Validate schema
            val validation = schemaRegistry.validate(event)
            if (!validation.isValid) {
                throw SchemaValidationException(validation.errors)
            }

            // 2. Check event size
            val eventSize = estimateEventSize(event)
            if (eventSize > config.maxEventSize) {
                throw EventTooLargeException(
                    eventId = event.eventId,
                    size = eventSize,
                    maxSize = config.maxEventSize
                )
            }

            // 3. Enrich with global properties
            val enrichedEvent = enrichEvent(event)

            // 4. Cache locally (with retry)
            cacheWithRetry(enrichedEvent)

            // 5. Route to destinations asynchronously
            routeEvent(enrichedEvent)
        }
    }

    override suspend fun recordBatch(events: List<TelemetryEvent>): Result<BatchResult> {
        val startTime = Clock.System.now().toEpochMilliseconds()
        val failures = mutableListOf<EventFailure>()
        var successCount = 0

        events.forEach { event ->
            record(event).fold(
                onSuccess = { successCount++ },
                onFailure = { error ->
                    failures.add(
                        EventFailure(
                            event = event,
                            reason = error.toFailureReason(),
                            message = error.message ?: "Unknown error",
                            retryable = error.isRetryable()
                        )
                    )
                }
            )
        }

        val processingTime = Clock.System.now().toEpochMilliseconds() - startTime

        return Result.success(
            BatchResult(
                totalEvents = events.size,
                successfulEvents = successCount,
                failedEvents = failures.size,
                failures = failures,
                processingTimeMs = processingTime
            )
        )
    }

    // ========================================================================
    // SPAN MANAGEMENT
    // ========================================================================

    override suspend fun startSpan(name: String, attributes: Map<String, Any>): EventSpan {
        val span = EventSpan(
            spanId = generateSpanId(),
            name = name,
            startTime = Clock.System.now(),
            endTime = null,
            attributes = attributes,
            events = emptyList()
        )
        activeSpans[span.spanId] = span

        logger.debug("Started span: ${span.spanId} - $name")
        return span
    }

    override suspend fun endSpan(spanId: String): Result<Unit> = runCatching {
        val span = activeSpans.remove(spanId)
            ?: throw SpanNotFoundException(spanId)

        if (span.endTime != null) {
            throw SpanAlreadyEndedException(spanId)
        }

        val completedSpan = span.copy(endTime = Clock.System.now())

        // Record span completion as event
        val spanEvent = createSpanEvent(completedSpan)
        record(spanEvent)

        logger.debug("Ended span: $spanId - duration: ${completedSpan.duration()}")
    }

    override suspend fun recordInSpan(spanId: String, event: TelemetryEvent): Result<Unit> =
        runCatching {
            val span = activeSpans[spanId] ?: throw SpanNotFoundException(spanId)

            // Add span context to event
            val enrichedEvent = event.withSpanContext(spanId)
            record(enrichedEvent).getOrThrow()

            // Update span's events list
            val updatedSpan = span.copy(events = span.events + event)
            activeSpans[spanId] = updatedSpan
        }

    // ========================================================================
    // CAMPAIGN TRACKING
    // ========================================================================

    override suspend fun registerCampaign(campaign: Campaign): Result<Unit> = runCatching {
        if (activeCampaigns.putIfAbsent(campaign.campaignId, campaign) != null) {
            throw CampaignAlreadyExistsException(campaign.campaignId)
        }
        logger.info("Registered campaign: ${campaign.campaignId} - ${campaign.name}")
    }

    override suspend fun getActiveCampaigns(): List<Campaign> {
        val now = Clock.System.now()
        return activeCampaigns.values().filter { campaign ->
            now >= campaign.startDate && now <= campaign.endDate
        }
    }

    override suspend fun getCampaignEvents(campaignId: String): Flow<TelemetryEvent> {
        return cache.retrieveByFilter(
            filter = EventFilter(campaignId = campaignId),
            limit = Int.MAX_VALUE
        ).asFlow()
    }

    // ========================================================================
    // PROPERTY MANAGEMENT
    // ========================================================================

    override suspend fun setGlobalProperties(properties: Map<String, Any>) {
        globalProperties.putAll(properties)
        logger.debug("Set global properties: ${properties.keys}")
    }

    override suspend fun setUserProperties(properties: Map<String, Any>) {
        globalProperties.putAll(properties.mapKeys { "user.${it.key}" })
        logger.debug("Set user properties for: ${sessionInfoProvider.get().userId}")
    }

    override suspend fun clearProperties(keys: Set<String>) {
        keys.forEach { globalProperties.remove(it) }
        logger.debug("Cleared properties: $keys")
    }

    // ========================================================================
    // SCHEMA MANAGEMENT
    // ========================================================================

    override suspend fun registerSchema(schema: EventSchema): Result<Unit> {
        return schemaRegistry.register(schema)
    }

    override suspend fun validateEvent(event: TelemetryEvent): ValidationResult {
        return schemaRegistry.validate(event)
    }

    override suspend fun migrateEvent(
        event: TelemetryEvent,
        targetVersion: SchemaVersion
    ): TelemetryEvent {
        return schemaRegistry.migrate(event, targetVersion)
    }

    // ========================================================================
    // QUERY & RETRIEVAL
    // ========================================================================

    @OptIn(ExperimentalTime::class)
    override suspend fun queryEvents(
        filter: EventFilter,
        timeRange: TimeRange,
        limit: Int
    ): Flow<TelemetryEvent> {
        return flow {
            val events = cache.retrieveByFilter(filter, limit)
            events.filter { event ->
                event.timestamp >= timeRange.start && event.timestamp <= timeRange.end
            }.forEach { emit(it) }
        }
    }

    override suspend fun getEventsByUser(
        userId: String,
        timeRange: TimeRange
    ): Flow<TelemetryEvent> {
        return queryEvents(
            filter = EventFilter(userId = userId),
            timeRange = timeRange,
            limit = Int.MAX_VALUE
        )
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getEventsBySession(sessionId: String): Flow<TelemetryEvent> {
        return queryEvents(
            filter = EventFilter(properties = mapOf("sessionId" to sessionId)),
            timeRange = TimeRange(
                start = Instant.DISTANT_PAST,
                end = Instant.DISTANT_FUTURE
            ),
            limit = Int.MAX_VALUE
        )
    }

    // ========================================================================
    // DESTINATION MANAGEMENT
    // ========================================================================

    override suspend fun flush(): Result<Unit> = runCatching {
        logger.info("Flushing observability repository...")

        // Flush cache
        cache.flush().getOrThrow()

        // Flush all destinations
        destinations.values.forEach { destination ->
            destination.flush().getOrThrow()
        }

        // Process remaining retry queue
        processRetryQueue()

        logger.info("Flush completed")
    }

    override suspend fun setDestinationEnabled(destination: Destination, enabled: Boolean) {
        destinations[destination]?.let {
            if (enabled) it.enable() else it.disable()
            logger.info("Destination $destination ${if (enabled) "enabled" else "disabled"}")
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getDestinationStatus(): Map<Destination, DestinationStatus> {
        return destinations.mapValues { (_, destination) ->
            val health = destination.healthCheck()
            val metrics = destination.getMetrics()

            DestinationStatus(
                isEnabled = destination.isEnabled,
                isHealthy = health.isHealthy,
                lastSync = metrics.lastSendTime,
                pendingEvents = metrics.queueSize
            )
        }
    }

    // ========================================================================
    // PRIVATE: EVENT ROUTING
    // ========================================================================

    private suspend fun routeEvent(event: TelemetryEvent) {
        val routing = getDestinationsForEvent(event)
        val context = buildDestinationContext(event)

        routing.forEach { dest ->
            destinations[dest]?.let { destination ->
                scope.launch {
                    sendWithRetry(event, destination, context)
                }
            }
        }
    }

    private suspend fun sendWithRetry(
        event: TelemetryEvent,
        destination: EventDestination,
        context: DestinationContext
    ) {
        try {
            val result = destination.send(event, context).getOrThrow()

            if (!result.accepted) {
                logger.warn(
                    "Event ${event.eventId} not accepted by ${destination.destination}: " +
                            "${result.metadata}"
                )
            } else {
                logger.debug(
                    "Event ${event.eventId} sent to ${destination.destination} " +
                            "in ${result.latencyMs}ms"
                )
            }
        } catch (e: Exception) {
            handleSendFailure(e, event, destination.destination, context)
        }
    }

    private suspend fun handleSendFailure(
        error: Throwable,
        event: TelemetryEvent,
        destination: Destination,
        context: DestinationContext
    ) {
        when (error) {
            is NetworkException -> {
                if (error.isRetryable()) {
                    queueForRetry(event, destination, error)
                } else {
                    logger.error("Non-retryable network error for $destination", error)
                }
            }

            is RateLimitException -> {
                queueForRetry(event, destination, error)
                logger.warn("Rate limited by $destination, queued for retry")
            }

            is DestinationException -> {
                if (error.temporary) {
                    queueForRetry(event, destination, error)
                } else {
                    logger.error("Permanent destination error for $destination", error)
                }
            }

            else -> {
                logger.error("Unexpected error sending to $destination", error)
            }
        }
    }

    private suspend fun queueForRetry(
        event: TelemetryEvent,
        destination: Destination,
        error: Throwable
    ) {
        val retryDelay = error.getRetryDelay()
        val retryableEvent = RetryableEvent(
            event = event,
            destination = destination,
            retryAfter = Clock.System.now() + retryDelay,
            attempts = 0,
            maxAttempts = config.maxRetryAttempts
        )

        retryQueue.add(retryableEvent)
        logger.info(
            "Queued event ${event.eventId} for retry to $destination " +
                    "after ${retryDelay.inWholeSeconds}s"
        )
    }

    // ========================================================================
    // PRIVATE: RETRY PROCESSOR
    // ========================================================================

    private fun startRetryProcessor() {
        scope.launch {
            while (isActive) {
                try {
                    processRetryQueue()
                } catch (e: Exception) {
                    logger.error("Error processing retry queue", e)
                }

                delay(config.retryProcessInterval)
            }
        }
    }

    private suspend fun processRetryQueue() {
        val readyEvents = retryQueue.getReady()

        if (readyEvents.isEmpty()) {
            return
        }

        logger.debug("Processing ${readyEvents.size} events from retry queue")

        readyEvents.forEach { retryableEvent ->
            scope.launch {
                processRetryableEvent(retryableEvent)
            }
        }
    }

    private suspend fun processRetryableEvent(retryableEvent: RetryableEvent) {
        val destination = destinations[retryableEvent.destination]
        if (destination == null) {
            logger.warn("Destination ${retryableEvent.destination} not found, removing from queue")
            retryQueue.remove(listOf(retryableEvent.event.eventId))
            return
        }

        val context = buildDestinationContext(retryableEvent.event)
            .copy(retryAttempt = retryableEvent.attempts + 1)

        try {
            val result = destination.send(retryableEvent.event, context).getOrThrow()

            if (result.accepted) {
                // Success! Remove from queue
                retryQueue.remove(listOf(retryableEvent.event.eventId))
                logger.info(
                    "Successfully retried event ${retryableEvent.event.eventId} " +
                            "to ${retryableEvent.destination} after ${retryableEvent.attempts + 1} attempts"
                )
            } else {
                // Not accepted but no exception
                retryQueue.markFailed(
                    retryableEvent.event.eventId,
                    DestinationRejectionException(
                        destination = retryableEvent.destination,
                        eventId = retryableEvent.event.eventId,
                        reason = "Event not accepted: ${result.metadata}"
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn(
                "Retry attempt ${retryableEvent.attempts + 1} failed " +
                        "for event ${retryableEvent.event.eventId}: ${e.message}"
            )
            retryQueue.markFailed(retryableEvent.event.eventId, e)
        }
    }

    private suspend fun restoreQueuedEvents() {
        // If using persistent queue, restore events on startup
        // This ensures events survive app restarts
        logger.info("Restoring queued events from persistent storage...")
        // Implementation depends on PersistentRetryQueue interface
    }

    // ========================================================================
    // PRIVATE: HELPER METHODS
    // ========================================================================

    private suspend fun buildDestinationContext(event: TelemetryEvent): DestinationContext {
        val now = Clock.System.now()
        val campaigns = activeCampaigns.values().filter { campaign ->
            now >= campaign.startDate &&
                    now <= campaign.endDate &&
                    campaign.targetEvents.contains(event::class.simpleName)
        }
        val sessionInfo = sessionInfoProvider.get()

        return DestinationContext(
            campaigns = campaigns,
            spanId = activeSpans.values().lastOrNull()?.spanId,
            sessionId = sessionInfo.sessionId,
            userId = sessionInfo.userId,
            globalProperties = globalProperties.toMap(),
            deviceInfo = deviceInfo,
            networkInfo = getCurrentNetworkInfo(),
            timestamp = Clock.System.now(),
            retryAttempt = 0,
            traceContext = buildTraceContext()
        )
    }

    private fun getDestinationsForEvent(event: TelemetryEvent): List<Destination> {
        return when (event) {
            is SystemEvent.Crash -> listOf(Destination.NEW_RELIC, Destination.LOCAL_DB)
            is SystemEvent.Error -> listOf(Destination.NEW_RELIC, Destination.LOCAL_DB)
            is SystemEvent.Performance -> listOf(Destination.NEW_RELIC)
            is AuditEvent -> listOf(Destination.CLOUD_STORAGE, Destination.LOCAL_DB)
            is AnalyticsEvent -> listOf(Destination.FIREBASE, Destination.MIXPANEL)
        }
    }

    private suspend fun enrichEvent(event: TelemetryEvent): TelemetryEvent {
        // Add global properties and context to event
        val enrichedProperties = event.properties.toMutableMap()

        // Add global properties
        enrichedProperties.putAll(globalProperties.toMap())

        // Add context
        val sessionInfo = sessionInfoProvider.get()
        sessionInfo.userId?.let { enrichedProperties["userId"] = it }
        sessionInfo.sessionId?.let { enrichedProperties["sessionId"] = it }

        // Add active span
        activeSpans.values().lastOrNull()?.let { span ->
            enrichedProperties["spanId"] = span.spanId
            enrichedProperties["spanName"] = span.name
        }

        // Add device info
        deviceInfo?.let { device ->
            enrichedProperties["deviceId"] = device.deviceId
            enrichedProperties["platform"] = device.platform
            enrichedProperties["osVersion"] = device.osVersion
            enrichedProperties["appVersion"] = device.appVersion
        }

        // Return new event with enriched properties
        // This is simplified - actual implementation would depend on event type
        return event // Would need proper copy with new properties
    }

    private fun estimateEventSize(event: TelemetryEvent): Long {
        return event.properties.entries.sumOf { (key, value) ->
            key.length + value.toString().length.toLong()
        } + 100 // overhead
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun cacheWithRetry(event: TelemetryEvent, maxRetries: Int = 3) {
        repeat(maxRetries) { attempt ->
            cache.store(event).fold(
                onSuccess = { return },
                onFailure = { error ->
                    when (error) {
                        is CacheFullException -> {
                            // Try to make space
                            cache.deleteOlderThan(
                                Clock.System.now() - 7.days
                            )
                            delay((50 * (attempt + 1)).toLong())
                        }

                        is CacheWriteException -> {
                            if (attempt < maxRetries - 1) {
                                delay((50 * (attempt + 1)).toLong())
                            } else {
                                throw error
                            }
                        }

                        else -> throw error
                    }
                }
            )
        }
    }

    private fun getCurrentNetworkInfo(): NetworkInfo? {
        // Platform-specific implementation
        // Would use Android ConnectivityManager or iOS Reachability
        return null
    }

    private suspend fun buildTraceContext(): TraceContext? {
        val currentSpan = activeSpans.values().lastOrNull()
        return currentSpan?.let {
            TraceContext(
                traceId = generateTraceId(),
                spanId = it.spanId,
                parentSpanId = null,
                traceFlags = 1
            )
        }
    }

    private fun createSpanEvent(span: EventSpan): TelemetryEvent {
        return SystemEvent.Performance(
            eventId = generateEventId(),
            timestamp = span.endTime ?: Clock.System.now(),
            properties = mapOf(
                "spanId" to span.spanId,
                "spanName" to span.name,
                "duration" to span.duration(),
                "attributes" to span.attributes
            ),
            schema = SchemaVersion("observability.span_completion", 1)
        )
    }

    private fun TelemetryEvent.withSpanContext(spanId: String): TelemetryEvent {
        // Add span context to event properties
        // This is simplified - actual implementation would depend on event type
        return this // Would need proper copy with span context
    }

    private fun EventSpan.duration(): Long {
        val end = endTime ?: Clock.System.now()
        return (end - startTime).inWholeMilliseconds
    }

    private fun generateSpanId(): String = Uuid.random().toString()
    private fun generateTraceId(): String = Uuid.random().toString()
    private fun generateEventId(): String = Uuid.random().toString()

}
