package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KasaBrandHeader
import com.example.ui.theme.KasaError
import com.example.ui.theme.KasaSuccess
import com.example.ui.theme.KasaWarmClay
import com.example.ui.theme.KasaWarmGold

@Composable
fun AuthScreen(
  viewModel: AuthViewModel,
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scrollState = rememberScrollState()
  val context = LocalContext.current

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("auth_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .imePadding()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // KASA Brand Header
      KasaBrandHeader(
        logoSize = 72.dp,
        showTagline = true,
        isAnimated = false
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Mode Selection Tabs (Sign In / Create Account)
      if (uiState.mode != AuthMode.FORGOT_PASSWORD) {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          TabRow(
            selectedTabIndex = if (uiState.mode == AuthMode.SIGN_IN) 0 else 1,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
              if (tabPositions.isNotEmpty()) {
                val index = if (uiState.mode == AuthMode.SIGN_IN) 0 else 1
                TabRowDefaults.SecondaryIndicator(
                  Modifier.tabIndicatorOffset(tabPositions[index]),
                  color = KasaWarmGold,
                  height = 3.dp
                )
              }
            },
            divider = {}
          ) {
            Tab(
              selected = uiState.mode == AuthMode.SIGN_IN,
              onClick = { viewModel.setMode(AuthMode.SIGN_IN) },
              modifier = Modifier
                .height(48.dp)
                .testTag("auth_tab_sign_in"),
              text = {
                Text(
                  text = "Sign In",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (uiState.mode == AuthMode.SIGN_IN) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            )
            Tab(
              selected = uiState.mode == AuthMode.SIGN_UP,
              onClick = { viewModel.setMode(AuthMode.SIGN_UP) },
              modifier = Modifier
                .height(48.dp)
                .testTag("auth_tab_sign_up"),
              text = {
                Text(
                  text = "Create Account",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (uiState.mode == AuthMode.SIGN_UP) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            )
          }
        }
      } else {
        // Forgot password top back header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewModel.setMode(AuthMode.SIGN_IN) },
            modifier = Modifier.testTag("auth_forgot_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Sign In"
            )
          }
          Text(
            text = "Reset Password",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // General error / banner if any
      AnimatedVisibility(visible = uiState.generalError != null) {
        uiState.generalError?.let { err ->
          Card(
            colors = CardDefaults.cardColors(
              containerColor = KasaError.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
              .testTag("auth_general_error_banner"),
            border = androidx.compose.foundation.BorderStroke(1.dp, KasaError.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = KasaError,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = KasaError
              )
            }
          }
        }
      }

      // Success banner if any
      AnimatedVisibility(visible = uiState.successMessage != null) {
        uiState.successMessage?.let { msg ->
          Card(
            colors = CardDefaults.cardColors(
              containerColor = KasaSuccess.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
              .testTag("auth_success_banner"),
            border = androidx.compose.foundation.BorderStroke(1.dp, KasaSuccess.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Success",
                tint = KasaSuccess,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = KasaSuccess
              )
            }
          }
        }
      }

      // Display Name input (Sign Up only)
      if (uiState.mode == AuthMode.SIGN_UP) {
        OutlinedTextField(
          value = uiState.displayName,
          onValueChange = viewModel::onDisplayNameChanged,
          label = { Text("Full Name") },
          placeholder = { Text("e.g. Kwame Mensah") },
          leadingIcon = {
            Icon(Icons.Outlined.Person, contentDescription = "Name")
          },
          isError = uiState.displayNameError != null,
          supportingText = uiState.displayNameError?.let { { Text(it, color = KasaError) } },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_name_input")
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Email input
      OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChanged,
        label = { Text("Email Address") },
        placeholder = { Text("name@example.com") },
        leadingIcon = {
          Icon(Icons.Outlined.Email, contentDescription = "Email")
        },
        isError = uiState.emailError != null,
        supportingText = uiState.emailError?.let { { Text(it, color = KasaError) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = if (uiState.mode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (uiState.mode == AuthMode.FORGOT_PASSWORD) {
              viewModel.submit(onAuthSuccess)
            }
          }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_email_input")
      )

      // Password input (Sign In / Sign Up only)
      if (uiState.mode != AuthMode.FORGOT_PASSWORD) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = uiState.password,
          onValueChange = viewModel::onPasswordChanged,
          label = { Text("Password") },
          leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = "Password")
          },
          trailingIcon = {
            IconButton(onClick = viewModel::togglePasswordVisibility) {
              Icon(
                imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = "Toggle password visibility"
              )
            }
          },
          visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          isError = uiState.passwordError != null,
          supportingText = uiState.passwordError?.let { { Text(it, color = KasaError) } },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (uiState.mode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (uiState.mode == AuthMode.SIGN_IN) {
                viewModel.submit(onAuthSuccess)
              }
            }
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_password_input")
        )
      }

      // Confirm Password (Sign Up only)
      if (uiState.mode == AuthMode.SIGN_UP) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = uiState.confirmPassword,
          onValueChange = viewModel::onConfirmPasswordChanged,
          label = { Text("Confirm Password") },
          leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = "Confirm Password")
          },
          trailingIcon = {
            IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
              Icon(
                imageVector = if (uiState.isConfirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = "Toggle confirm password visibility"
              )
            }
          },
          visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          isError = uiState.confirmPasswordError != null,
          supportingText = uiState.confirmPasswordError?.let { { Text(it, color = KasaError) } },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = { viewModel.submit(onAuthSuccess) }
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_confirm_password_input")
        )
      }

      // Forgot Password link (Sign In only)
      if (uiState.mode == AuthMode.SIGN_IN) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = { viewModel.setMode(AuthMode.FORGOT_PASSWORD) },
            modifier = Modifier.testTag("auth_forgot_password_button")
          ) {
            Text(
              text = "Forgot password?",
              style = MaterialTheme.typography.bodySmall,
              color = KasaWarmGold
            )
          }
        }
      } else {
        Spacer(modifier = Modifier.height(20.dp))
      }

      // Main Submit Button
      Button(
        onClick = { viewModel.submit(onAuthSuccess) },
        enabled = !uiState.isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("auth_submit_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        if (uiState.isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp
          )
        } else {
          val buttonTitle = when (uiState.mode) {
            AuthMode.SIGN_IN -> "Sign In"
            AuthMode.SIGN_UP -> "Create Account"
            AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
          }
          Text(
            text = buttonTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Alternate Action Divider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
          text = "or",
          modifier = Modifier.padding(horizontal = 14.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Google Sign In Option
      OutlinedButton(
        onClick = { viewModel.continueWithGoogle(context, onAuthSuccess) },
        enabled = !uiState.isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("auth_google_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.AccountCircle,
          contentDescription = null,
          tint = KasaWarmGold,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Continue with Google",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Guest Explorer Option
      TextButton(
        onClick = { viewModel.continueAsGuest(onAuthSuccess) },
        modifier = Modifier.testTag("auth_guest_button")
      ) {
        Text(
          text = "Explore as Guest",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
