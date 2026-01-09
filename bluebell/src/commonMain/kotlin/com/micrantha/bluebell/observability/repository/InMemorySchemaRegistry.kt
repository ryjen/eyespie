package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.domain.MutableThreadSafeMap
import com.micrantha.bluebell.observability.domain.IncompatibleSchemaException
import com.micrantha.bluebell.observability.domain.MigrationNotFoundException
import com.micrantha.bluebell.observability.domain.SchemaAlreadyExistsException
import com.micrantha.bluebell.observability.domain.SchemaMigration
import com.micrantha.bluebell.observability.domain.SchemaNotFoundException
import com.micrantha.bluebell.observability.domain.SchemaRegistry
import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.PropertyType
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.ValidationError
import com.micrantha.bluebell.observability.entity.ValidationErrorReason
import com.micrantha.bluebell.observability.entity.ValidationResult
import com.micrantha.bluebell.observability.entity.ValidationWarning
import com.micrantha.bluebell.observability.info
import com.micrantha.bluebell.observability.logger

// Schema Registry Implementation
class InMemorySchemaRegistry : SchemaRegistry {
    private val schemas = MutableThreadSafeMap<SchemaVersion, EventSchema>()
    private val migrations =
        MutableThreadSafeMap<Pair<SchemaVersion, SchemaVersion>, SchemaMigration>()
    private val logger by logger()

    override suspend fun register(schema: EventSchema): Result<Unit> = runCatching {
        val version = SchemaVersion(schema.name, schema.version)

        if (schemas.containsKey(version)) {
            throw SchemaAlreadyExistsException(version)
        }

        val previousVersion = getLatestSchema(schema.name)
        if (previousVersion != null) {
            validateBackwardCompatibility(previousVersion, schema)
        }

        schemas[version] = schema
        logger.info("Registered schema: ${schema.name} v${schema.version}")
    }

    override suspend fun getSchema(version: SchemaVersion): EventSchema? {
        return schemas[version]
    }

    override suspend fun getLatestSchema(name: String): EventSchema? {
        return schemas.values()
            .filter { it.name == name }
            .maxByOrNull { it.version }
    }

    override suspend fun getAllVersions(name: String): List<EventSchema> {
        return schemas.values()
            .filter { it.name == name }
            .sortedBy { it.version }
    }

    override suspend fun deprecateSchema(version: SchemaVersion): Result<Unit> = runCatching {
        val schema = schemas[version]
            ?: throw SchemaNotFoundException(version)

        schemas[version] = schema.copy(deprecated = true)
        logger.info("Deprecated schema: $version")
    }

    override suspend fun validate(event: TelemetryEvent): ValidationResult {
        val schema = schemas[event.schema]
            ?: return ValidationResult(
                isValid = false,
                errors = listOf(
                    ValidationError(
                        field = "schema",
                        reason = ValidationErrorReason.SchemaNotFound,
                        message = "Schema ${event.schema} not found in registry"
                    )
                )
            )

        return validateEventAgainstSchema(event, schema)
    }

    suspend fun registerMigration(
        from: SchemaVersion,
        to: SchemaVersion,
        migration: SchemaMigration
    ) {
        migrations[from to to] = migration
        logger.info("Registered migration: $from -> $to")
    }

    override suspend fun migrate(event: TelemetryEvent, version: SchemaVersion): TelemetryEvent {
        val migration = migrations[event.schema to version]
            ?: throw MigrationNotFoundException(event.schema, version)

        return migration.migrate(event)
    }

    private fun validateEventAgainstSchema(
        event: TelemetryEvent,
        schema: EventSchema
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()

        // Check required fields
        schema.required.forEach { requiredField ->
            if (!event.properties.containsKey(requiredField)) {
                errors.add(
                    ValidationError(
                        field = requiredField,
                        reason = ValidationErrorReason.MissingRequiredField,
                        message = "Required field '$requiredField' is missing"
                    )
                )
            }
        }

        // Validate property types
        event.properties.forEach { (field, value) ->
            val expectedType = schema.properties[field]
            if (expectedType != null && !expectedType.matches(value)) {
                errors.add(
                    ValidationError(
                        field = field,
                        reason = ValidationErrorReason.InvalidType,
                        message = "Field '$field' has invalid type"
                    )
                )
            }
        }

        // Check for deprecated fields
        schema.deprecatedFields?.forEach { deprecatedField ->
            if (event.properties.containsKey(deprecatedField)) {
                warnings.add(
                    ValidationWarning(
                        field = deprecatedField,
                        message = "Field '$deprecatedField' is deprecated",
                        suggestion = schema.fieldReplacements?.get(deprecatedField)
                    )
                )
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateBackwardCompatibility(old: EventSchema, new: EventSchema) {
        old.required.forEach { requiredField ->
            if (!new.properties.containsKey(requiredField)) {
                throw IncompatibleSchemaException(
                    oldVersion = SchemaVersion(old.name, old.version),
                    newVersion = SchemaVersion(new.name, new.version),
                    incompatibilities = listOf("New schema removes required field '$requiredField'")
                )
            }
        }
    }

    private fun PropertyType.matches(value: Any): Boolean = when (this) {
        is PropertyType.String -> value is String
        is PropertyType.Number -> value is Number
        is PropertyType.Boolean -> value is Boolean
        is PropertyType.Enum -> value is String && value in this.values
        is PropertyType.Object -> true
    }
}
