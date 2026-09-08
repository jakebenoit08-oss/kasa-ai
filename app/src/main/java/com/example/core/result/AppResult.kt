package com.example.core.result

import com.example.core.error.AppError

sealed interface AppResult<out T> {
  data class Success<out T>(val data: T) : AppResult<T>
  data class Error(val error: AppError) : AppResult<Nothing>
  data object Loading : AppResult<Nothing>

  val isSuccess: Boolean get() = this is Success
  val isError: Boolean get() = this is Error
  val isLoading: Boolean get() = this is Loading

  fun getOrNull(): T? = (this as? Success)?.data

  fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
    is Success -> Success(transform(data))
    is Error -> this
    is Loading -> Loading
  }
}
