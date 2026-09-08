package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShortText
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ai.SupportedLanguage
import com.example.core.config.AppConfig
import com.example.data.model.ConversationalTone
import com.example.data.model.LearningStyle
import com.example.data.model.ResponseLength
import com.example.data.model.memory.MemoryItem
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.components.KasaEditNameDialog
import com.example.ui.components.KasaInfoDialog
import com.example.ui.components.KasaOutlinedCard
import com.example.ui.components.KasaPrimaryButton
import com.example.ui.components.KasaTextButton
import com.example.ui.theme.KasaSpacing
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  onSignedOut: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = androidx.compose.ui.platform.LocalContext.current
  var showBackendUrlDialog by remember { mutableStateOf(false) }
  var backendUrlInput by remember { mutableStateOf(AppConfig.getMusicBackendUrl(context)) }

  LaunchedEffect(state.statusMessage) {
    state.statusMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.dismissStatusMessage()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Settings & Personalization",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.testTag("settings_screen"),
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp)
          .padding(horizontal = KasaSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(KasaSpacing.medium),
      ) {
        // Section: Account Foundation
        item {
          SectionHeader(title = "Account & Identity")
        }

        item {
          val user = state.user
          KasaOutlinedCard(testTag = "settings_account_card") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  val initials = (user?.displayName ?: "K").take(2).uppercase()
                  Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = user?.displayName ?: "KASA Member",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (user?.isLocalGuest == true) {
                      KasaBadge(text = "Guest Session")
                    } else {
                      KasaBadge(text = "Firebase Auth")
                    }
                  }
                  if (!user?.email.isNullOrBlank()) {
                    Text(
                      text = user?.email ?: "",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "UID: ${user?.id ?: "Loading..."}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  )
                }
                IconButton(
                  onClick = { viewModel.setEditNameDialogVisible(true) },
                  modifier = Modifier.testTag("settings_edit_name_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit Name",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

              // Account Action Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedButton(
                  onClick = { viewModel.setSignOutDialogVisible(true) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("settings_sign_out_button"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "Sign Out", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                  onClick = { viewModel.setDeleteAccountDialogVisible(true) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("settings_delete_account_button"),
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                  ),
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                  Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = "Delete Account",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(text = "Delete Account", style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }
        }

        // Section: Personalization & AI Behavior
        item {
          SectionHeader(title = "AI Response Personalization")
        }

        item {
          KasaOutlinedCard(testTag = "settings_personalization_card") {
            Column(verticalArrangement = Arrangement.spacedBy(KasaSpacing.small)) {
              // Response Length
              SettingsRow(
                icon = Icons.Outlined.Tune,
                title = "Response Length",
                subtitle = when (state.settings?.responseLength) {
                  ResponseLength.SHORT -> "Short & Direct • Quick concise answers"
                  ResponseLength.BALANCED -> "Balanced • Clear explanations with context"
                  ResponseLength.DETAILED -> "Detailed • In-depth, comprehensive breakdown"
                  null -> "Balanced"
                },
                onClick = { viewModel.setResponseLengthDialogVisible(true) },
                testTag = "settings_response_length_row",
              )

              // Conversational Tone
              SettingsRow(
                icon = Icons.Outlined.RecordVoiceOver,
                title = "Conversational Tone",
                subtitle = when (state.settings?.conversationalTone) {
                  ConversationalTone.FRIENDLY -> "Friendly & Warm • Engaging, warm Ghanaian tone"
                  ConversationalTone.NEUTRAL -> "Neutral • Objective, standard, direct"
                  ConversationalTone.PROFESSIONAL -> "Professional • Formal, structured, business-ready"
                  null -> "Friendly & Warm"
                },
                onClick = { viewModel.setConversationalToneDialogVisible(true) },
                testTag = "settings_conversational_tone_row",
              )

              // Learning Style
              SettingsRow(
                icon = Icons.Outlined.School,
                title = "Study & Learning Style",
                subtitle = when (state.settings?.learningStyle) {
                  LearningStyle.SIMPLE -> "Simple & Intuitive • Clear analogies and plain language"
                  LearningStyle.STEP_BY_STEP -> "Step-by-Step • Methodical progression and numbered steps"
                  LearningStyle.EXAMPLES_FIRST -> "Examples First • Practical Ghanaian cases before theory"
                  null -> "Simple & Intuitive"
                },
                onClick = { viewModel.setLearningStyleDialogVisible(true) },
                testTag = "settings_learning_style_row",
              )
            }
          }
        }

        // Section: KASA Memory & Saved Preferences
        item {
          SectionHeader(title = "KASA Memory & Transparent Context")
        }

        item {
          KasaCard(testTag = "settings_memory_card") {
            Column(verticalArrangement = Arrangement.spacedBy(KasaSpacing.small)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Psychology,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Explicit Memory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = if (state.settings?.memoryEnabled != false) {
                      "Active (${state.memories.size} saved facts)"
                    } else {
                      "Paused (No memories injected)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = state.settings?.memoryEnabled ?: true,
                  onCheckedChange = { viewModel.toggleMemory(it) },
                  modifier = Modifier.testTag("settings_memory_switch"),
                )
              }

              Text(
                text = "KASA only remembers things you explicitly ask it to remember (e.g., 'Remember that I am preparing for WASSCE'). Silent conversational recording is disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = { viewModel.setManageMemoriesDialogVisible(true) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("settings_manage_memories_button"),
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Manage (${state.memories.size})")
                }

                if (state.memories.isNotEmpty()) {
                  OutlinedButton(
                    onClick = { viewModel.setClearAllMemoriesDialogVisible(true) },
                    modifier = Modifier.testTag("settings_clear_memories_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Delete,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                  }
                }
              }
            }
          }
        }

        // Section: Appearance & General Preferences
        item {
          SectionHeader(title = "System & Localization")
        }

        item {
          KasaOutlinedCard(testTag = "settings_preferences_card") {
            Column(verticalArrangement = Arrangement.spacedBy(KasaSpacing.small)) {
              // Theme Selector
              SettingsRow(
                icon = Icons.Outlined.DarkMode,
                title = "Appearance",
                subtitle = when (state.settings?.themeMode) {
                  ThemeMode.SYSTEM -> "System Default"
                  ThemeMode.LIGHT -> "Light Mode"
                  ThemeMode.DARK -> "Dark Mode"
                  null -> "System Default"
                },
                onClick = { viewModel.setThemeDialogVisible(true) },
                testTag = "settings_theme_row",
              )

              // Language Selector
              SettingsRow(
                icon = Icons.Outlined.Language,
                title = "Primary Language",
                subtitle = state.settings?.preferredLanguage?.let { "${it.displayName} (${it.nativeName})" } ?: "English",
                onClick = { viewModel.setLanguageDialogVisible(true) },
                testTag = "settings_language_row",
              )

              // Haptics Toggle
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = KasaSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Vibration,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Haptic Feedback",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    text = "Tactile responses on interactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = state.settings?.hapticFeedbackEnabled ?: true,
                  onCheckedChange = { viewModel.toggleHapticFeedback(it) },
                  modifier = Modifier.testTag("settings_haptics_switch"),
                )
              }
            }
          }
        }

        // Section: AI Credential Architecture & Security Audit
        item {
          SectionHeader(title = "AI Security & Credential Architecture")
        }

        item {
          KasaCard(
            onClick = { viewModel.setSecurityAuditDialogVisible(true) },
            testTag = "settings_security_audit_card",
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Security Architecture Audit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  KasaBadge(text = "Phase 6 Verified")
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Safe key retrieval via BuildConfig, prompt injection defense, and strict user database boundaries.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Section: Configuration & System Status
        item {
          SectionHeader(title = "Environment & Build Status")
        }

        item {
          KasaOutlinedCard(testTag = "settings_config_card") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              ConfigInfoRow(label = "Application", value = AppConfig.APP_NAME)
              ConfigInfoRow(label = "Phase", value = AppConfig.PHASE_IDENTIFIER)
              ConfigInfoRow(label = "Version", value = AppConfig.APP_VERSION)
              ConfigInfoRow(label = "Gemini AI Engine", value = AppConfig.getApiKeyStatus())
              ConfigInfoRow(label = "Music Backend", value = AppConfig.getMusicBackendUrl(context))
              ConfigInfoRow(label = "Build Environment", value = if (AppConfig.isDebug()) "Development (Debug)" else "Production (Release)")
            }
          }
        }

        // Section: Music Studio Backend
        item {
          SectionHeader(title = "Music Studio Backend")
        }

        item {
          KasaCard(
            onClick = {
              backendUrlInput = AppConfig.getMusicBackendUrl(context)
              showBackendUrlDialog = true
            },
            testTag = "settings_music_backend_card",
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Music Server URL (Tap to configure)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = AppConfig.getMusicBackendUrl(context),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Production: Render HTTPS Web Service. Development: Emulator (10.0.2.2) or ADB reverse.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit Backend URL",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        // Section: About
        item {
          SectionHeader(title = "About KASA AI")
        }

        item {
          KasaCard(testTag = "settings_about_card") {
            Text(
              text = AppConfig.APP_NAME,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = AppConfig.APP_TAGLINE,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "KASA AI is Ghana's AI companion, supporting multilingual text chat, real-time voice, curriculum-aligned study lessons, creative image generation, and transparent user-controlled memory.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.height(KasaSpacing.large))
        }
      }
    }

    // Dialog: Edit Name
    if (state.showEditNameDialog) {
      KasaEditNameDialog(
        currentName = state.user?.displayName ?: "",
        onDismiss = { viewModel.setEditNameDialogVisible(false) },
        onSave = { viewModel.updateDisplayName(it) },
      )
    }

    // Dialog: Theme Picker
    if (state.showThemeDialog) {
      ThemePickerDialog(
        currentMode = state.settings?.themeMode ?: ThemeMode.SYSTEM,
        onSelectMode = { viewModel.updateThemeMode(it) },
        onDismiss = { viewModel.setThemeDialogVisible(false) },
      )
    }

    // Dialog: Language Picker
    if (state.showLanguageDialog) {
      LanguagePickerDialog(
        currentLanguage = state.settings?.preferredLanguage ?: SupportedLanguage.ENGLISH,
        onSelectLanguage = { viewModel.updateLanguage(it) },
        onDismiss = { viewModel.setLanguageDialogVisible(false) },
      )
    }

    // Dialog: Response Length Picker
    if (state.showResponseLengthDialog) {
      ResponseLengthDialog(
        currentLength = state.settings?.responseLength ?: ResponseLength.BALANCED,
        onSelect = { viewModel.updateResponseLength(it) },
        onDismiss = { viewModel.setResponseLengthDialogVisible(false) },
      )
    }

    // Dialog: Conversational Tone Picker
    if (state.showConversationalToneDialog) {
      ConversationalToneDialog(
        currentTone = state.settings?.conversationalTone ?: ConversationalTone.FRIENDLY,
        onSelect = { viewModel.updateConversationalTone(it) },
        onDismiss = { viewModel.setConversationalToneDialogVisible(false) },
      )
    }

    // Dialog: Learning Style Picker
    if (state.showLearningStyleDialog) {
      LearningStyleDialog(
        currentStyle = state.settings?.learningStyle ?: LearningStyle.SIMPLE,
        onSelect = { viewModel.updateLearningStyle(it) },
        onDismiss = { viewModel.setLearningStyleDialogVisible(false) },
      )
    }

    // Dialog: Manage Memories
    if (state.showManageMemoriesDialog) {
      ManageMemoriesDialog(
        memories = state.memories,
        onDeleteMemory = { viewModel.deleteMemory(it) },
        onAddNew = { viewModel.setAddMemoryDialogVisible(true) },
        onDismiss = { viewModel.setManageMemoriesDialogVisible(false) },
      )
    }

    // Dialog: Add Manual Memory
    if (state.showAddMemoryDialog) {
      AddMemoryDialog(
        onDismiss = { viewModel.setAddMemoryDialogVisible(false) },
        onSave = { viewModel.addManualMemory(it) },
      )
    }

    // Dialog: Clear All Memories Confirmation
    if (state.showClearAllMemoriesDialog) {
      AlertDialog(
        onDismissRequest = { viewModel.setClearAllMemoriesDialogVisible(false) },
        title = {
          Text(
            text = "Delete All Memories?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = "This will permanently delete all ${state.memories.size} saved facts and preferences from your device. This cannot be undone.",
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          Button(
            onClick = { viewModel.clearAllMemories() },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.testTag("confirm_clear_memories_button"),
          ) {
            Text("Delete All")
          }
        },
        dismissButton = {
          KasaTextButton(text = "Cancel", onClick = { viewModel.setClearAllMemoriesDialogVisible(false) })
        },
        modifier = Modifier.testTag("clear_all_memories_dialog"),
      )
    }

    // Dialog: Security Architecture Audit
    if (state.showSecurityAuditDialog) {
      SecurityAuditDialog(
        onDismiss = { viewModel.setSecurityAuditDialogVisible(false) },
      )
    }

    // Dialog: Sign Out Confirmation
    if (state.showSignOutDialog) {
      AlertDialog(
        onDismissRequest = { viewModel.setSignOutDialogVisible(false) },
        title = {
          Text(
            text = "Sign Out?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = "Are you sure you want to sign out? Your saved data is stored securely on your account.",
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          Button(
            onClick = { viewModel.signOut(onSignedOut) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.testTag("confirm_sign_out_button"),
          ) {
            Text("Sign Out")
          }
        },
        dismissButton = {
          KasaTextButton(text = "Cancel", onClick = { viewModel.setSignOutDialogVisible(false) })
        },
        modifier = Modifier.testTag("sign_out_dialog"),
      )
    }

    // Dialog: Delete Account Confirmation
    if (state.showDeleteAccountDialog) {
      AlertDialog(
        onDismissRequest = { viewModel.setDeleteAccountDialogVisible(false) },
        title = {
          Text(
            text = "Delete Account & All Data?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
          )
        },
        text = {
          Text(
            text = "This will permanently delete your user account and wipe ALL conversations, generated images, study history, quizzes, and personalized memories from this device and authentication servers. This action is irreversible.",
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          Button(
            onClick = { viewModel.deleteAccount(onSignedOut) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.testTag("confirm_delete_account_button"),
          ) {
            Text("Delete Permanently")
          }
        },
        dismissButton = {
          KasaTextButton(text = "Cancel", onClick = { viewModel.setDeleteAccountDialogVisible(false) })
        },
        modifier = Modifier.testTag("delete_account_dialog"),
      )
    }

    if (showBackendUrlDialog) {
      AlertDialog(
        onDismissRequest = { showBackendUrlDialog = false },
        title = {
          Text(
            text = "Music Server Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Enter the server endpoint for KASA Music. Must end with a slash '/':",
              style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
              value = backendUrlInput,
              onValueChange = { backendUrlInput = it },
              singleLine = true,
              label = { Text("Server URL") },
              modifier = Modifier.fillMaxWidth().testTag("music_backend_url_input"),
            )
            Text(
              text = "Quick Presets:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = {
                  backendUrlInput = if (!AppConfig.MUSIC_BACKEND_PRODUCTION_URL.contains("YOUR-RENDER-SERVICE")) {
                    AppConfig.MUSIC_BACKEND_PRODUCTION_URL
                  } else {
                    "https://YOUR-SERVICE.onrender.com/"
                  }
                },
                modifier = Modifier.weight(1f),
              ) {
                Text("Render\nHTTPS", style = MaterialTheme.typography.labelSmall)
              }
              OutlinedButton(
                onClick = { backendUrlInput = AppConfig.MUSIC_BACKEND_EMULATOR_URL },
                modifier = Modifier.weight(1f),
              ) {
                Text("Emulator\n(10.0.2.2)", style = MaterialTheme.typography.labelSmall)
              }
              OutlinedButton(
                onClick = { backendUrlInput = AppConfig.MUSIC_BACKEND_LOCALHOST_URL },
                modifier = Modifier.weight(1f),
              ) {
                Text("ADB Reverse\n(127.0.0.1)", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val formatted = if (backendUrlInput.trim().endsWith("/")) {
                backendUrlInput.trim()
              } else {
                "${backendUrlInput.trim()}/"
              }
              AppConfig.setMusicBackendUrl(context, formatted)
              showBackendUrlDialog = false
            },
            modifier = Modifier.testTag("save_music_backend_button"),
          ) {
            Text("Save")
          }
        },
        dismissButton = {
          KasaTextButton(text = "Cancel", onClick = { showBackendUrlDialog = false })
        },
      )
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.padding(top = KasaSpacing.small),
  )
}

@Composable
private fun SettingsRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = KasaSpacing.small)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ConfigInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun ResponseLengthDialog(
  currentLength: ResponseLength,
  onSelect: (ResponseLength) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Response Length Preference",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        listOf(
          ResponseLength.SHORT to ("Short" to "Direct, fast, and concise answers with minimal preamble."),
          ResponseLength.BALANCED to ("Balanced" to "Clear, natural explanations with appropriate context."),
          ResponseLength.DETAILED to ("Detailed" to "Comprehensive, deep-dive answers with full background."),
        ).forEach { (length, info) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = currentLength == length,
                onClick = { onSelect(length) },
              )
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentLength == length,
              onClick = { onSelect(length) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = info.first,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = info.second,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Close", onClick = onDismiss)
    },
    modifier = Modifier.testTag("response_length_dialog"),
  )
}

@Composable
private fun ConversationalToneDialog(
  currentTone: ConversationalTone,
  onSelect: (ConversationalTone) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Conversational Tone",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        listOf(
          ConversationalTone.FRIENDLY to ("Friendly & Warm" to "Engaging, conversational, uses warm Ghanaian hospitality."),
          ConversationalTone.NEUTRAL to ("Neutral" to "Balanced, direct, objective, and matter-of-fact."),
          ConversationalTone.PROFESSIONAL to ("Professional" to "Formal, structured, and business-focused."),
        ).forEach { (tone, info) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = currentTone == tone,
                onClick = { onSelect(tone) },
              )
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentTone == tone,
              onClick = { onSelect(tone) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = info.first,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = info.second,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Close", onClick = onDismiss)
    },
    modifier = Modifier.testTag("conversational_tone_dialog"),
  )
}

@Composable
private fun LearningStyleDialog(
  currentStyle: LearningStyle,
  onSelect: (LearningStyle) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = listOf(
    Triple(LearningStyle.SIMPLE, "Simple & Intuitive", "Everyday analogies and clear, accessible explanations."),
    Triple(LearningStyle.STEP_BY_STEP, "Step-by-Step", "Methodical breakdown with numbered stages and proofs."),
    Triple(LearningStyle.EXAMPLES_FIRST, "Examples First", "Practical everyday Ghanaian illustrations before theory."),
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Study & Learning Style",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        options.forEach { (style, title, desc) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = currentStyle == style,
                onClick = { onSelect(style) },
              )
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentStyle == style,
              onClick = { onSelect(style) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Close", onClick = onDismiss)
    },
    modifier = Modifier.testTag("learning_style_dialog"),
  )
}

@Composable
private fun ManageMemoriesDialog(
  memories: List<MemoryItem>,
  onDeleteMemory: (String) -> Unit,
  onAddNew: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "KASA Memories (${memories.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Explicitly saved facts and preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onAddNew,
          modifier = Modifier.testTag("add_memory_icon_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Add Memory",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    },
    text = {
      if (memories.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Outlined.BookmarkBorder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No saved memories yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Say 'Remember that...' in chat or tap '+' above.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(memories, key = { it.id }) { memory ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = memory.memoryText,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                )
                val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(memory.createdAt))
                Text(
                  text = "Saved $dateStr",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
              }
              IconButton(
                onClick = { onDeleteMemory(memory.id) },
                modifier = Modifier
                  .size(32.dp)
                  .testTag("delete_memory_${memory.id}"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Delete,
                  contentDescription = "Delete memory",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Done", onClick = onDismiss)
    },
    modifier = Modifier.testTag("manage_memories_dialog"),
  )
}

@Composable
private fun AddMemoryDialog(
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Add Custom Preference",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        Text(
          text = "Enter a fact or preference you want KASA to keep in mind (e.g. 'I am studying for BECE' or 'I prefer explanations in Twi').",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Memory Fact") },
          placeholder = { Text("e.g. I am a science student in Kumasi") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_memory_input"),
          maxLines = 3,
        )
      }
    },
    confirmButton = {
      KasaPrimaryButton(
        text = "Save Memory",
        onClick = {
          if (text.isNotBlank()) {
            onSave(text.trim())
          }
        },
        enabled = text.isNotBlank(),
        testTag = "save_manual_memory_button",
      )
    },
    dismissButton = {
      KasaTextButton(text = "Cancel", onClick = onDismiss)
    },
    modifier = Modifier.testTag("add_memory_dialog"),
  )
}

@Composable
private fun SecurityAuditDialog(
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "AI Credential & Security Audit",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Text(
            text = "1. API Credential Architecture",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "The Gemini API key is injected at build time from the AI Studio Secrets panel / .env into BuildConfig.GEMINI_API_KEY. It is never logged in Logcat, never transmitted to analytics, and never displayed in cleartext.",
            style = MaterialTheme.typography.bodySmall,
          )
        }

        item {
          Text(
            text = "2. Client-Side Disclosure & Honest Tradeoff",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Because KASA AI operates strictly within a GH₵0 budget without intermediate cloud proxy servers, API calls route directly from the client to Google's official Gemini endpoint. In production enterprise releases, this should route through an authenticated backend proxy to protect keys against decompilation.",
            style = MaterialTheme.typography.bodySmall,
          )
        }

        item {
          Text(
            text = "3. Multi-Tenant Local Isolation",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "All chat history, study quizzes, generated images, and memories are partitioned strictly by 'userId' in local SQLite (Room). No data is shared across sessions or stored in global singletons.",
            style = MaterialTheme.typography.bodySmall,
          )
        }

        item {
          Text(
            text = "4. Prompt Injection & Memory Sanitization",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Memories are injected into system prompts inside an explicit untrusted boundary with strict filtering of sensitive data (passwords, PINs, OTPs, financial identifiers) before saving.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Understood", onClick = onDismiss)
    },
    modifier = Modifier.testTag("security_audit_dialog"),
  )
}

@Composable
private fun ThemePickerDialog(
  currentMode: ThemeMode,
  onSelectMode: (ThemeMode) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Choose Appearance",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        listOf(
          ThemeMode.SYSTEM to "System Default",
          ThemeMode.LIGHT to "Light Mode",
          ThemeMode.DARK to "Dark Mode",
        ).forEach { (mode, label) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = currentMode == mode,
                onClick = { onSelectMode(mode) },
              )
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentMode == mode,
              onClick = { onSelectMode(mode) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Close", onClick = onDismiss)
    },
    modifier = Modifier.testTag("theme_picker_dialog"),
  )
}

@Composable
private fun LanguagePickerDialog(
  currentLanguage: SupportedLanguage,
  onSelectLanguage: (SupportedLanguage) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "Language Preference",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Guides AI response style while adapting to how you speak.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(
          items = SupportedLanguage.entries.toList(),
          key = { it.code },
        ) { language ->
          val detail = com.example.core.ai.context.GhanaianContextConfig.LANGUAGE_SUPPORT_DETAILS.find { it.language == language }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .selectable(
                selected = currentLanguage == language,
                onClick = { onSelectLanguage(language) },
              )
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = currentLanguage == language,
              onClick = { onSelectLanguage(language) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "${language.displayName} (${language.nativeName})",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                if (detail != null) {
                  val badgeColor = when (detail.status) {
                    com.example.core.ai.context.LanguageCapabilityStatus.STRONG_SUPPORT -> MaterialTheme.colorScheme.primary
                    com.example.core.ai.context.LanguageCapabilityStatus.PARTIAL_SUPPORT -> MaterialTheme.colorScheme.secondary
                    com.example.core.ai.context.LanguageCapabilityStatus.EXPERIMENTAL -> MaterialTheme.colorScheme.outline
                  }
                  Text(
                    text = detail.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Medium,
                  )
                }
              }
              if (detail != null) {
                Text(
                  text = detail.notes,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      KasaTextButton(text = "Close", onClick = onDismiss)
    },
    modifier = Modifier.testTag("language_picker_dialog"),
  )
}
