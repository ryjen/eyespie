package com.micrantha.bluebell.observability.entity

sealed interface ValidationErrorReason {
    object MissingRequiredField : ValidationErrorReason
    object InvalidType : ValidationErrorReason
    object InvalidEnumValue : ValidationErrorReason
    object SchemaNotFound : ValidationErrorReason
    object VersionMismatch : ValidationErrorReason
    data class CustomValidation(val rule: String) : ValidationErrorReason
}
