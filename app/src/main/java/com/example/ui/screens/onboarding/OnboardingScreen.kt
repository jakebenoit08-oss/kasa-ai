package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.KasaBrandHeader
import com.example.ui.components.KasaLogo
import com.example.ui.theme.KasaDeepCharcoal
import com.example.ui.theme.KasaWarmClay
import com.example.ui.theme.KasaWarmGold
import com.example.ui.theme.KasaWarmIvory
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel,
  onFinishOnboarding: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pagerState = rememberPagerState(pageCount = { 4 })
  val coroutineScope = rememberCoroutineScope()

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("onboarding_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Navigation / Skip Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          KasaLogo(size = 28.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "KASA AI",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (pagerState.currentPage < 3) {
          TextButton(
            onClick = {
              viewModel.completeOnboarding(onFinishOnboarding)
            },
            modifier = Modifier.testTag("onboarding_skip_button")
          ) {
            Text(
              text = "Skip",
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Spacer(modifier = Modifier.width(48.dp))
        }
      }

      // Pager Content
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 12.dp)
      ) { page ->
        when (page) {
          0 -> OnboardingPageWelcome()
          1 -> OnboardingPageCapabilities()
          2 -> OnboardingPagePersonalization()
          3 -> OnboardingPageControl()
        }
      }

      // Bottom Section: Indicators & Action Button
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Dot Indicators
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 24.dp)
        ) {
          repeat(4) { index ->
            val isSelected = pagerState.currentPage == index
            Box(
              modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(8.dp)
                .width(if (isSelected) 24.dp else 8.dp)
                .clip(CircleShape)
                .background(
                  if (isSelected) KasaWarmGold else MaterialTheme.colorScheme.outlineVariant
                )
            )
          }
        }

        // Action Buttons
        if (pagerState.currentPage == 3) {
          Button(
            onClick = {
              viewModel.completeOnboarding(onFinishOnboarding)
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("onboarding_get_started_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
          ) {
            Text(
              text = "Get Started",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Get Started"
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (pagerState.currentPage > 0) {
              OutlinedButton(
                onClick = {
                  coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                  }
                },
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("onboarding_back_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text(text = "Back")
              }
              Spacer(modifier = Modifier.width(12.dp))
            }

            Button(
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
              },
              modifier = Modifier
                .weight(if (pagerState.currentPage > 0) 1.5f else 1f)
                .height(50.dp)
                .testTag("onboarding_next_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Text(
                text = "Next",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next",
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingPageWelcome() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    KasaBrandHeader(
      logoSize = 88.dp,
      showTagline = false,
      isAnimated = true
    )

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "Your intelligent companion, built with Ghana in mind.",
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp
      ),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Experience fluent conversational intelligence grounded in Ghanaian culture, education, local languages, and everyday real-time context.",
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Language badges
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf("Twi", "Fante", "Ga", "Ewe", "English").forEach { lang ->
        Surface(
          modifier = Modifier.padding(horizontal = 4.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Text(
            text = lang,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun OnboardingPageCapabilities() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "Five Powerful Capabilities",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "All designed to assist, educate, and create with cultural precision.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    val capabilities = listOf(
      CapabilityItem(
        icon = Icons.AutoMirrored.Outlined.Chat,
        title = "Chat",
        description = "Natural streaming reasoning and multilingual dialogues."
      ),
      CapabilityItem(
        icon = Icons.Outlined.Mic,
        title = "Live Voice",
        description = "Hands-free real-time voice streaming with ultra-low latency."
      ),
      CapabilityItem(
        icon = Icons.Outlined.AutoAwesome,
        title = "Create",
        description = "AI image generation celebrating African art and design."
      ),
      CapabilityItem(
        icon = Icons.Outlined.School,
        title = "Study",
        description = "GES-aligned syllabus quizzes and step-by-step tutoring."
      ),
      CapabilityItem(
        icon = Icons.Outlined.TravelExplore,
        title = "Search",
        description = "Live web search grounding with verified source citations."
      ),
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      capabilities.forEach { item ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = KasaWarmGold.copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = KasaWarmGold,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

private data class CapabilityItem(
  val icon: ImageVector,
  val title: String,
  val description: String,
)

@Composable
private fun OnboardingPagePersonalization() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = CircleShape,
      color = KasaWarmClay.copy(alpha = 0.12f),
      modifier = Modifier.size(72.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Outlined.Tune,
          contentDescription = "Personalization",
          tint = KasaWarmClay,
          modifier = Modifier.size(36.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Make KASA Work Your Way",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Customize how KASA responds to match your daily workflow and learning pace.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    val settingsFeatures = listOf(
      "Preferred Ghanaian & international languages",
      "Response length: Short, Balanced, or Detailed",
      "Tone: Friendly, Neutral, or Academic Professional",
      "Pedagogy: Step-by-step or Examples-first tutoring",
      "Optional user-controlled Memory for tailored assistance"
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      settingsFeatures.forEach { feature ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = KasaWarmGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
private fun OnboardingPageControl() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      shape = CircleShape,
      color = KasaWarmGold.copy(alpha = 0.15f),
      modifier = Modifier.size(72.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Outlined.Lock,
          contentDescription = "Control and Privacy",
          tint = KasaWarmGold,
          modifier = Modifier.size(36.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "You're in Full Control",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Your privacy and data boundaries are respected by design.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      ),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant
      )
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        ControlItem(
          title = "Personal Account Partitioning",
          subtitle = "Your chats, images, and quizzes are strictly isolated to your account."
        )
        ControlItem(
          title = "Transparent Memory Controls",
          subtitle = "View, add, edit, or wipe all AI memories at any time in Settings."
        )
        ControlItem(
          title = "Account Ownership",
          subtitle = "Easily update your profile or delete your account whenever you choose."
        )
      }
    }
  }
}

@Composable
private fun ControlItem(
  title: String,
  subtitle: String,
) {
  Row(verticalAlignment = Alignment.Top) {
    Icon(
      imageVector = Icons.Default.Check,
      contentDescription = null,
      tint = KasaWarmClay,
      modifier = Modifier
        .size(18.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
