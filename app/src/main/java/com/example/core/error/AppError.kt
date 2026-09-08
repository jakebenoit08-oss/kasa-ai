package com.example.core.error

sealed class AppError(
  open val message: String,
  open val cause: Throwable? = null,
) {
  data class NetworkError(
    override val message: String = "Network connection unavailable. Please check your internet connection.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class ServiceUnavailable(
    override val message: String = "The requested service is temporarily unavailable. Please try again later.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class ConfigurationError(
    override val message: String = "Service configuration is missing or incomplete.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class ValidationError(
    override val message: String,
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class Unauthorized(
    override val message: String = "Authentication is required to perform this action.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class AiEngineError(
    override val message: String = "AI generation encountered an error.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class StorageError(
    override val message: String = "Local storage operation failed.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)

  data class UnknownError(
    override val message: String = "Something went wrong. Please try again.",
    override val cause: Throwable? = null,
  ) : AppError(message, cause)
}
