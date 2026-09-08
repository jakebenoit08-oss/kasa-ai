package com.example.ui.screens.placeholders

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.navigation.Screen
import com.example.ui.components.KasaBadge
import com.example.ui.components.KasaCard
import com.example.ui.components.KasaOutlinedCard
import com.example.ui.components.KasaPrimaryButton
import com.example.ui.theme.KasaSpacing

data class CapabilityPlaceholderSpec(
  val screen: Screen,
  val title: String,
  val phaseTarget: String,
  val heroHeadline: String,
  val description: String,
  val architectureStatus: String,
  val plannedCapabilities: List<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
  spec: CapabilityPlaceholderSpec,
  onNavigateToChat: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = spec.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            KasaBadge(
              text = "Phase 0 • Deferred",
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.testTag("placeholder_settings_button"),
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
    modifier = modifier.testTag("placeholder_${spec.screen.route}"),
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
        item {
          Spacer(modifier = Modifier.height(KasaSpacing.small))
          // Hero Overview Card
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("placeholder_hero_${spec.screen.route}"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(KasaSpacing.large),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = spec.screen.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(30.dp),
                )
              }
              Spacer(modifier = Modifier.height(KasaSpacing.medium))
              Text(
                text = spec.heroHeadline,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(KasaSpacing.small))
              Text(
                text = spec.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(KasaSpacing.medium))
              KasaBadge(
                text = spec.phaseTarget,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        // Architectural Readiness Status
        item {
          KasaCard(testTag = "placeholder_architecture_card") {
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Architectural Readiness",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = spec.architectureStatus,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Planned Capabilities
        item {
          Text(
            text = "Planned Capabilities",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        item {
          KasaOutlinedCard(testTag = "placeholder_features_card") {
            Column(verticalArrangement = Arrangement.spacedBy(KasaSpacing.small)) {
              spec.plannedCapabilities.forEach { capability ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = capability,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }

        // Navigation Action to Chat
        item {
          KasaPrimaryButton(
            text = "Go to Chat Interface",
            onClick = onNavigateToChat,
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "placeholder_go_chat_button",
          )
          Spacer(modifier = Modifier.height(KasaSpacing.large))
        }
      }
    }
  }
}
