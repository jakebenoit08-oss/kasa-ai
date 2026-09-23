package com.example.ui.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Verified
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserMusicCredits
import com.example.ui.components.KasaBadge
import com.example.ui.theme.KasaSpacing

@Composable
fun MusicUpgradeDialog(
  credits: UserMusicCredits?,
  isUpgrading: Boolean,
  checkoutReference: String?,
  billingMessage: String?,
  onDismiss: () -> Unit,
  onSelectPlan: (planId: String) -> Unit,
  onVerifyPayment: (reference: String?) -> Unit,
) {
  val userTier = credits?.tier?.lowercase() ?: "free"
  var selectedPlan by remember(userTier) {
    mutableStateOf(if (userTier == "plus") "pro" else "plus")
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .testTag("music_upgrade_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .padding(KasaSpacing.large)
          .verticalScroll(rememberScrollState()),
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(22.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "KASA AI Premium",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Get more from KASA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("music_upgrade_dismiss_button"),
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Close",
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Status Card
        val userTier = credits?.tier?.lowercase() ?: "free"
        val isOwner = credits?.isOwner == true
        val isPlus = !isOwner && userTier == "plus"
        val isPro = !isOwner && userTier == "pro"
        val isFree = !isOwner && !isPlus && !isPro

        Card(
          modifier = Modifier.fillMaxWidth().testTag("music_current_status_card"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Current Status",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = when {
                  isOwner -> "Owner Access"
                  isPro -> "KASA Pro"
                  isPlus -> "KASA Plus"
                  else -> "Free Tier"
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
              )
            }

            val badgeText = when {
              isOwner -> "Unlimited"
              credits != null -> "${credits.remaining} / ${credits.limit} left"
              else -> "1 credit / 30 days"
            }
            KasaBadge(
              text = badgeText,
              containerColor = Color(0xFFD4AF37).copy(alpha = 0.2f),
              contentColor = Color(0xFF8A6D00),
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isOwner) {
          // Owner notice: permanent unrestricted access, do not push to purchase
          Card(
            modifier = Modifier.fillMaxWidth().testTag("music_owner_access_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f),
            ),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(24.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Owner Access • Unlimited",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF8A6D00),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Your account has permanent developer & owner access. All music generation and AI capabilities are unrestricted.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else {
          // Plan Selector
          Text(
            text = "Select a Plan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(8.dp))

          // FREE PLAN CARD
          PlanCard(
            title = "FREE",
            price = "GH₵ 0",
            period = "default plan",
            creditsText = "1 music generation per 30 days",
            features = listOf(
              "1 AI music generation per 30-day cycle",
              "Dual song variations per prompt",
              "Standard generation queue",
            ),
            isSelected = selectedPlan == "free",
            badge = if (isFree) "Current Plan" else null,
            onClick = { selectedPlan = "free" },
            testTag = "music_plan_free",
          )

          Spacer(modifier = Modifier.height(10.dp))

          // PLUS PLAN CARD
          PlanCard(
            title = "PLUS",
            price = "GH₵ 49",
            period = "per month",
            creditsText = "5 music generations per 30 days",
            features = listOf(
              "5 AI music generations per 30 days",
              "Dual song variations per prompt",
              "Higher allowance than Free (5 vs 1)",
              "Twi, Fante, Ga, Ewe & English vocals",
              "Highlife, Afrobeats, Gospel & modern genres",
            ),
            isSelected = selectedPlan == "plus",
            badge = if (isPlus) "Current Plan" else null,
            onClick = { selectedPlan = "plus" },
            testTag = "music_plan_plus",
          )

          Spacer(modifier = Modifier.height(10.dp))

          // PRO PLAN CARD
          PlanCard(
            title = "PRO",
            price = "GH₵ 99",
            period = "per month",
            creditsText = "15 music generations per 30 days",
            features = listOf(
              "Everything in Plus",
              "15 AI music generations per 30 days",
              "Priority fast-track generation queue",
              "Full commercial music use rights",
              "Advanced prompt engineering & styles",
            ),
            isSelected = selectedPlan == "pro",
            badge = if (isPro) "Current Plan" else if (!isPlus) "Most Popular" else null,
            onClick = { selectedPlan = "pro" },
            testTag = "music_plan_pro",
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Payment Status Banner (if any)
          if (!billingMessage.isNullOrBlank()) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              ),
            ) {
              Text(
                text = billingMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(10.dp),
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          // Dynamic CTA Button
          val isFreePlanSelected = selectedPlan == "free"
          val isCurrentPlanSelected = (isFree && selectedPlan == "free") ||
            (isPlus && selectedPlan == "plus") ||
            (isPro && selectedPlan == "pro")

          val ctaText = when {
            isPro -> "Pro Active • 15 credits / cycle"
            isCurrentPlanSelected -> "Current Plan"
            selectedPlan == "plus" -> "Upgrade to Plus — GH₵49/mo"
            selectedPlan == "pro" -> "Upgrade to Pro — GH₵99/mo"
            else -> "Select a Plan"
          }

          val isCtaEnabled = !isUpgrading && !isCurrentPlanSelected && !isFreePlanSelected && !isPro

          Button(
            onClick = {
              if (selectedPlan == "plus" || selectedPlan == "pro") {
                onSelectPlan(selectedPlan)
              }
            },
            enabled = isCtaEnabled,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("music_upgrade_checkout_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFD4AF37),
              contentColor = Color(0xFF1C1B1F),
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          ) {
            if (isUpgrading) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF1C1B1F),
                strokeWidth = 2.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Processing...")
            } else {
              if (isCtaEnabled) {
                Icon(
                  imageVector = Icons.Outlined.Lock,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
              }
              Text(
                text = ctaText,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          // If a reference was initialized, show verify button
          if (!checkoutReference.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = { onVerifyPayment(checkoutReference) },
              enabled = !isUpgrading,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("music_upgrade_verify_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("I've Paid — Refresh My Balance")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Security footer
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Secured by Paystack • MTN MoMo, Telecel, AT & Cards",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PlanCard(
  title: String,
  price: String,
  period: String,
  creditsText: String,
  features: List<String>,
  isSelected: Boolean,
  badge: String?,
  onClick: () -> Unit,
  testTag: String,
) {
  val borderColor = if (isSelected) Color(0xFFD4AF37) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  val backgroundColor = if (isSelected) Color(0xFFD4AF37).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = creditsText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A6D00),
            fontWeight = FontWeight.SemiBold,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          if (badge != null) {
            KasaBadge(
              text = badge,
              containerColor = Color(0xFFD4AF37),
              contentColor = Color(0xFF1C1B1F),
            )
            Spacer(modifier = Modifier.height(2.dp))
          }
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = price,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = " /mo",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(10.dp))

      // Features
      features.forEach { feat ->
        Row(
          modifier = Modifier.padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = feat,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}
