package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.config.AppConfig
import com.example.navigation.Screen
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.components.KasaEmptyState
import com.example.ui.components.KasaOutlinedCard
import com.example.ui.components.KasaPrimaryButton
import com.example.ui.theme.KasaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onNavigateToScreen: (Screen) -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.kasa_brand_logo),
              contentDescription = "KASA AI Logo",
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = AppConfig.APP_NAME,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            KasaBadge(text = "Phase 8")
          }
        },
        actions = {
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("home_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Settings",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        ),
      )
    },
    modifier = modifier.testTag("home_screen"),
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
        // Welcome & Branding Banner
        item {
          Spacer(modifier = Modifier.height(KasaSpacing.small))
          WelcomeHeroCard(
            greeting = state.greeting,
            tagline = AppConfig.APP_TAGLINE,
            onStartChat = { onNavigateToScreen(Screen.Chat) },
          )
        }

        // About Foundation Section
        item {
          FoundationOverviewCard()
        }

        // Future Capabilities Roadmap Section
        item {
          Text(
            text = "Active Capabilities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = KasaSpacing.small),
          )
        }

        item {
          FutureCapabilityItem(
            title = "KASA Live Voice",
            subtitle = "Real-time bidirectional spoken conversation with natural barge-in",
            phaseTag = "Live",
            icon = Icons.Outlined.Mic,
            onClick = { onNavigateToScreen(Screen.Live) },
            testTag = "capability_live_card",
          )
        }

        item {
          FutureCapabilityItem(
            title = "Creative Studio",
            subtitle = "Cultural image, wallpaper, and multimodal creative generation",
            phaseTag = "Live",
            icon = Icons.Outlined.AutoAwesome,
            onClick = { onNavigateToScreen(Screen.Create) },
            testTag = "capability_create_card",
          )
        }

        item {
          FutureCapabilityItem(
            title = "Study Companion",
            subtitle = "Curriculum tutoring, step-by-step solutions, and practice quizzes",
            phaseTag = "Live",
            icon = Icons.Outlined.School,
            onClick = { onNavigateToScreen(Screen.Study) },
            testTag = "capability_study_card",
          )
        }

        // Recent Activity Empty State
        item {
          Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = KasaSpacing.small),
          )
        }

        item {
          if (state.recentConversations.isEmpty()) {
            KasaOutlinedCard(
              testTag = "home_empty_activity_card",
            ) {
              KasaEmptyState(
                title = "No Recent Conversations",
                description = "Your chat history is strictly isolated to your local account session and will appear here when created.",
                icon = Icons.AutoMirrored.Outlined.Chat,
                actionButton = {
                  KasaPrimaryButton(
                    text = "Open Chat Interface",
                    onClick = { onNavigateToScreen(Screen.Chat) },
                    testTag = "home_empty_chat_cta",
                  )
                },
              )
            }
          }
        }

        // Data Isolation & Privacy Notice
        item {
          DataIsolationFooterCard()
          Spacer(modifier = Modifier.height(KasaSpacing.large))
        }
      }
    }
  }
}

@Composable
private fun WelcomeHeroCard(
  greeting: String,
  tagline: String,
  onStartChat: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("welcome_hero_card"),
    shape = RoundedCornerShape(KasaSpacing.cardCornerRadius),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.padding(KasaSpacing.large),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = greeting,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
        KasaBadge(
          text = "AI READY",
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = tagline,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(KasaSpacing.small))
      Text(
        text = "An intelligent AI companion designed for Ghana and Africa. Establishing a robust, scalable foundation.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(KasaSpacing.large))
      KasaPrimaryButton(
        text = "Start Conversation",
        onClick = onStartChat,
        leadingIcon = {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Chat,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        },
        testTag = "hero_start_chat_button",
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun FoundationOverviewCard() {
  KasaCard(testTag = "foundation_overview_card") {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.TipsAndUpdates,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "Phase 0: Foundation Stage",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Clean architecture, user isolation, and design system ready for Phase 1 AI engine connection.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun FutureCapabilityItem(
  title: String,
  subtitle: String,
  phaseTag: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String,
) {
  KasaOutlinedCard(
    onClick = onClick,
    testTag = testTag,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.width(8.dp))
          KasaBadge(text = phaseTag)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
        contentDescription = "View placeholder info",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun DataIsolationFooterCard() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(KasaSpacing.small),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Outlined.Security,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "Strict per-user data isolation active. Session data is never shared across accounts.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
