package com.example.ui.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.config.AppConfig
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.MessageDeliveryStatus
import com.example.data.model.MessageSender
import com.example.data.model.search.SearchSource
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaPrimaryButton
import com.example.ui.theme.KasaSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  viewModel: ChatViewModel,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()
  val listState = rememberLazyListState()
  var showMenu by remember { mutableStateOf(false) }

  // Auto-scroll when messages or streaming updates change
  LaunchedEffect(state.messages.size, state.streamingContent) {
    if (state.messages.isNotEmpty()) {
      listState.animateScrollToItem(state.messages.size - 1)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "KASA AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.width(8.dp))
              KasaBadge(
                text = if (state.isAiEngineConnected) "Gemini" else "Offline Ready",
                containerColor = if (state.isAiEngineConnected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (state.isAiEngineConnected) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
            Text(
              text = state.activeConversation?.title ?: "New Conversation",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = { viewModel.toggleConversationsDrawer() },
            modifier = Modifier.testTag("chat_history_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.History,
              contentDescription = "Conversation History",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.startNewConversation() },
            modifier = Modifier.testTag("chat_new_session_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Add,
              contentDescription = "New Conversation",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.testTag("chat_options_menu_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Chat Options",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false },
            ) {
              if (state.activeConversation != null) {
                DropdownMenuItem(
                  text = { Text("Rename Chat") },
                  onClick = {
                    showMenu = false
                    viewModel.openRenameDialog()
                  },
                  leadingIcon = {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                  },
                  modifier = Modifier.testTag("menu_rename_chat"),
                )
                DropdownMenuItem(
                  text = { Text("Delete Chat") },
                  onClick = {
                    showMenu = false
                    viewModel.openDeleteDialog()
                  },
                  leadingIcon = {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                  },
                  modifier = Modifier.testTag("menu_delete_chat"),
                )
              }
              DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                  showMenu = false
                  onNavigateToSettings()
                },
                leadingIcon = {
                  Icon(Icons.Outlined.Settings, contentDescription = null)
                },
                modifier = Modifier.testTag("menu_chat_settings"),
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    bottomBar = {
      ChatBottomBar(
        inputText = state.inputText,
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onSend = { viewModel.sendMessage() },
        isGenerating = state.isGenerating,
        isSearchModeEnabled = state.isSearchModeEnabled,
        onToggleSearchMode = { viewModel.toggleSearchMode() },
      )
    },
    modifier = modifier.testTag("chat_screen"),
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // Global Error Banner with Retry
      AnimatedVisibility(
        visible = state.errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        if (state.errorMessage != null) {
          ChatErrorBanner(
            errorMessage = state.errorMessage!!,
            canRetry = state.failedPromptForRetry != null,
            onRetry = { viewModel.retryLastFailedMessage() },
            onDismiss = { viewModel.dismissError() },
          )
        }
      }

      // Web Search In-Progress Banner
      AnimatedVisibility(
        visible = state.isSearchingWeb || state.searchStatusText != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KasaSpacing.medium, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("web_search_active_banner"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
              imageVector = Icons.Outlined.TravelExplore,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = state.searchStatusText ?: "Searching the web...",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }

      // Memory Candidate Confirmation Banner
      AnimatedVisibility(
        visible = state.pendingMemoryCandidate != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        state.pendingMemoryCandidate?.let { candidate ->
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = KasaSpacing.medium, vertical = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .testTag("memory_candidate_banner"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Save to KASA Memory?",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "\"$candidate\"",
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              TextButton(
                onClick = { viewModel.confirmSaveMemory() },
                modifier = Modifier.testTag("confirm_save_memory_button"),
              ) {
                Text("Save", fontWeight = FontWeight.Bold)
              }
              TextButton(
                onClick = { viewModel.dismissPendingMemory() },
                modifier = Modifier.testTag("dismiss_memory_button"),
              ) {
                Text("Dismiss")
              }
            }
          }
        }
      }

      // Memory Saved / Cleared Feedback Banner
      AnimatedVisibility(
        visible = state.memoryFeedbackMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        state.memoryFeedbackMessage?.let { feedback ->
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = KasaSpacing.medium, vertical = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .testTag("memory_feedback_banner"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = { viewModel.dismissMemoryFeedback() }) {
                Text("OK", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      if (state.messages.isEmpty()) {
        ChatEmptyStateView(
          onStarterPromptSelected = { prompt ->
            viewModel.sendMessage(prompt)
          },
          isSearchModeEnabled = state.isSearchModeEnabled,
          onToggleSearchMode = { viewModel.toggleSearchMode() },
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = KasaSpacing.medium),
          verticalArrangement = Arrangement.spacedBy(KasaSpacing.small),
        ) {
          item { Spacer(modifier = Modifier.height(KasaSpacing.small)) }

          items(
            items = state.messages,
            key = { it.id },
          ) { message ->
            val isCurrentlyStreamingThis = state.isGenerating && state.streamingMessageId == message.id
            val displayContent = if (isCurrentlyStreamingThis && state.streamingContent.isNotEmpty()) {
              state.streamingContent
            } else {
              message.content
            }

            ChatMessageBubble(
              message = message,
              displayContent = displayContent,
              isStreaming = isCurrentlyStreamingThis,
              onRetry = { viewModel.retryLastFailedMessage() },
            )
          }

          item { Spacer(modifier = Modifier.height(KasaSpacing.small)) }
        }
      }
    }

    // Conversations History Bottom Sheet
    if (state.showConversationsDrawer) {
      ConversationsBottomSheet(
        conversations = state.conversations,
        activeConversationId = state.activeConversation?.id,
        onSelectConversation = { conv ->
          viewModel.selectConversation(conv)
        },
        onNewConversation = {
          viewModel.startNewConversation()
        },
        onRenameConversation = { conv ->
          viewModel.openRenameDialog(conv)
        },
        onDeleteConversation = { conv ->
          viewModel.openDeleteDialog(conv)
        },
        onDismiss = { viewModel.setConversationsDrawer(false) },
      )
    }

    // Rename Dialog
    if (state.showRenameDialog && state.renameTargetConversation != null) {
      RenameConversationDialog(
        currentTitle = state.renameTargetConversation!!.title,
        onConfirm = { newTitle -> viewModel.confirmRename(newTitle) },
        onDismiss = { viewModel.dismissRenameDialog() },
      )
    }

    // Delete Confirmation Dialog
    if (state.showDeleteConfirmDialog && state.deleteTargetConversation != null) {
      DeleteConversationDialog(
        title = state.deleteTargetConversation!!.title,
        onConfirm = { viewModel.confirmDelete() },
        onDismiss = { viewModel.dismissDeleteDialog() },
      )
    }
  }
}

@Composable
private fun ChatBottomBar(
  inputText: String,
  onInputTextChanged: (String) -> Unit,
  onSend: () -> Unit,
  isGenerating: Boolean,
  isSearchModeEnabled: Boolean,
  onToggleSearchMode: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    tonalElevation = 1.dp,
    modifier = Modifier.imePadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = KasaSpacing.medium, vertical = KasaSpacing.small),
    ) {
      // Search Mode Toggle Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = isSearchModeEnabled,
          onClick = onToggleSearchMode,
          label = {
            Text(
              text = if (isSearchModeEnabled) "Web Search ON" else "Web Search",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSearchModeEnabled) FontWeight.Bold else FontWeight.Normal,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Outlined.Language,
              contentDescription = "Web Search Grounding",
              modifier = Modifier.size(16.dp),
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
          ),
          modifier = Modifier.testTag("chat_search_mode_toggle"),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = onInputTextChanged,
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field"),
          placeholder = {
            Text(
              text = if (isGenerating) {
                "KASA is responding..."
              } else if (isSearchModeEnabled) {
                "Search the web or ask about current events..."
              } else {
                "Type a message..."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
          },
          maxLines = 4,
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
          ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        val canSend = inputText.isNotBlank() && !isGenerating

        IconButton(
          onClick = onSend,
          enabled = canSend,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("chat_send_button"),
        ) {
          if (isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Send,
              contentDescription = "Send Message",
              tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatEmptyStateView(
  onStarterPromptSelected: (String) -> Unit,
  isSearchModeEnabled: Boolean,
  onToggleSearchMode: () -> Unit,
) {
  val starterPrompts = listOf(
    "What is the current dollar to cedi exchange rate?" to "Search current Bank of Ghana and market rates",
    "What are the latest news headlines in Ghana today?" to "Search real-time Ghanaian and global news",
    "Explain this academic topic to me." to "Break down complex concepts simply",
    "Teach me something new." to "Learn fascinating facts or history",
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = KasaSpacing.large, vertical = KasaSpacing.medium),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Outlined.AutoAwesome,
        contentDescription = "KASA AI",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
    }

    Spacer(modifier = Modifier.height(KasaSpacing.medium))

    Text(
      text = AppConfig.APP_NAME,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = AppConfig.APP_TAGLINE,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(KasaSpacing.large))

    Text(
      text = "Get started with an example:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
    )

    Spacer(modifier = Modifier.height(KasaSpacing.medium))

    starterPrompts.forEach { (prompt, subtitle) ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp)
          .clip(RoundedCornerShape(16.dp))
          .clickable { onStarterPromptSelected(prompt) }
          .testTag("starter_prompt_${prompt.take(10).replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = prompt,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(
  message: ChatMessage,
  displayContent: String,
  isStreaming: Boolean,
  onRetry: () -> Unit,
) {
  val isUser = message.sender == MessageSender.USER
  val isFailed = message.deliveryStatus == MessageDeliveryStatus.FAILED
  val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
  val formattedTime = timeFormatter.format(Date(message.timestamp))

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("chat_message_${message.id}"),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
  ) {
    if (!isUser) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = "KASA",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(10.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "KASA AI",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (message.isWebSearch) {
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(10.dp),
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Web Grounded",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .widthIn(max = 340.dp)
        .clip(
          RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 20.dp,
          )
        )
        .background(
          if (isUser) {
            MaterialTheme.colorScheme.primary
          } else if (isFailed) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
          } else {
            MaterialTheme.colorScheme.surface
          }
        )
        .then(
          if (!isUser && !isFailed) {
            Modifier.background(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
            )
          } else Modifier
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Column {
        if (displayContent.isBlank() && isStreaming) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (message.isWebSearch) "Searching the web..." else "Thinking...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Text(
            text = displayContent,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = if (isUser) {
              MaterialTheme.colorScheme.onPrimary
            } else if (isFailed) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.onSurface
            },
          )
        }

        // Render Verified Source Cards if present
        if (!isUser && message.searchSources.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          SearchSourcesView(sources = message.searchSources)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = (if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
          )
          if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Outlined.CheckCircle,
              contentDescription = "Delivered",
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
          } else if (isFailed) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = "Failed",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(14.dp),
            )
          }
        }

        if (isFailed) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .clickable { onRetry() }
              .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = "Retry",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Retry",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchSourcesView(
  sources: List<SearchSource>,
) {
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp)
      .testTag("search_sources_container"),
  ) {
    Text(
      text = "Sources & Verification:",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(6.dp))

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      sources.take(6).forEachIndexed { index, source ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                context.startActivity(intent)
              } catch (_: Exception) {
                // Ignore if browser is not available
              }
            }
            .testTag("search_source_card_$index"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Language,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = source.domain.ifBlank { source.title },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
              contentDescription = "Open Link",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(10.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatErrorBanner(
  errorMessage: String,
  canRetry: Boolean,
  onRetry: () -> Unit,
  onDismiss: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = KasaSpacing.medium, vertical = KasaSpacing.small)
      .clip(RoundedCornerShape(12.dp)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.weight(1f),
      )
      if (canRetry) {
        TextButton(onClick = onRetry) {
          Text("Retry", fontWeight = FontWeight.Bold)
        }
      }
      TextButton(onClick = onDismiss) {
        Text("Dismiss")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationsBottomSheet(
  conversations: List<Conversation>,
  activeConversationId: String?,
  onSelectConversation: (Conversation) -> Unit,
  onNewConversation: () -> Unit,
  onRenameConversation: (Conversation) -> Unit,
  onDeleteConversation: (Conversation) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = KasaSpacing.medium)
        .padding(bottom = KasaSpacing.large),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Conversations",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        KasaPrimaryButton(
          text = "New Chat",
          onClick = {
            onNewConversation()
            onDismiss()
          },
          leadingIcon = {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          },
          testTag = "bottomsheet_new_chat_button",
        )
      }

      Spacer(modifier = Modifier.height(KasaSpacing.medium))

      if (conversations.isEmpty()) {
        Text(
          text = "No saved conversations yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = KasaSpacing.large),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(conversations, key = { it.id }) { conv ->
            val isSelected = conv.id == activeConversationId
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onSelectConversation(conv)
                  onDismiss()
                }
                .testTag("conversation_item_${conv.id}"),
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                  MaterialTheme.colorScheme.surface
                }
              ),
              border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = conv.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (!conv.lastMessageSnippet.isNullOrBlank()) {
                    Text(
                      text = conv.lastMessageSnippet,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(
                    onClick = { onRenameConversation(conv) },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Edit,
                      contentDescription = "Rename",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                  IconButton(
                    onClick = { onDeleteConversation(conv) },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RenameConversationDialog(
  currentTitle: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var text by remember { mutableStateOf(currentTitle) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename Conversation") },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("rename_conversation_input"),
        placeholder = { Text("Enter conversation title") },
      )
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(text) },
        enabled = text.isNotBlank(),
        modifier = Modifier.testTag("confirm_rename_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
private fun DeleteConversationDialog(
  title: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Delete Conversation") },
    text = {
      Text("Are you sure you want to permanently delete \"$title\" and all its messages?")
    },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
