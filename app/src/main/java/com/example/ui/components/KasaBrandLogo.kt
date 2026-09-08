package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KasaDeepCharcoal
import com.example.ui.theme.KasaObsidian
import com.example.ui.theme.KasaWarmClay
import com.example.ui.theme.KasaWarmGold
import com.example.ui.theme.KasaWarmGoldLight
import com.example.ui.theme.KasaWarmIvory

/**
 * KASA Brand Emblem.
 * 
 * An original, minimalist emblem representing conversation (speech arcs), intelligence (golden core),
 * connection, and Ghanaian warmth.
 */
@Composable
fun KasaLogo(
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  withContainer: Boolean = false,
  isAnimated: Boolean = false,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "kasa_logo_pulse")
  val pulseScale by if (isAnimated) {
    infiniteTransition.animateFloat(
      initialValue = 0.95f,
      targetValue = 1.05f,
      animationSpec = infiniteRepeatable(
        animation = tween(2200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "pulse_scale"
    )
  } else {
    androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
  }

  val content = @Composable {
    Canvas(
      modifier = Modifier
        .size(size)
        .testTag("kasa_brand_logo")
    ) {
      val w = this.size.width
      val h = this.size.height
      val cx = w / 2f
      val cy = h / 2f

      // Background ambient aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            KasaWarmGold.copy(alpha = 0.22f),
            KasaWarmClay.copy(alpha = 0.08f),
            Color.Transparent
          ),
          center = Offset(cx, cy),
          radius = w * 0.48f * pulseScale
        ),
        radius = w * 0.48f * pulseScale,
        center = Offset(cx, cy)
      )

      val strokeWidth = (w * 0.08f).coerceAtLeast(2f)

      // Left Speech / Harmonic Arc
      val leftPath = Path().apply {
        moveTo(w * 0.32f, h * 0.32f)
        cubicTo(
          w * 0.18f, h * 0.44f,
          w * 0.18f, h * 0.56f,
          w * 0.32f, h * 0.68f
        )
      }
      drawPath(
        path = leftPath,
        brush = Brush.verticalGradient(
          colors = listOf(KasaWarmClay, KasaWarmGold),
          startY = h * 0.32f,
          endY = h * 0.68f
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Right Speech / Harmonic Arc
      val rightPath = Path().apply {
        moveTo(w * 0.68f, h * 0.32f)
        cubicTo(
          w * 0.82f, h * 0.44f,
          w * 0.82f, h * 0.56f,
          w * 0.68f, h * 0.68f
        )
      }
      drawPath(
        path = rightPath,
        brush = Brush.verticalGradient(
          colors = listOf(KasaWarmGoldLight, KasaWarmClay),
          startY = h * 0.32f,
          endY = h * 0.68f
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Top Convergence Arc
      val topPath = Path().apply {
        moveTo(w * 0.36f, h * 0.28f)
        quadraticTo(
          cx, h * 0.18f,
          w * 0.64f, h * 0.28f
        )
      }
      drawPath(
        path = topPath,
        brush = Brush.horizontalGradient(
          colors = listOf(KasaWarmClay, KasaWarmGoldLight),
          startX = w * 0.36f,
          endX = w * 0.64f
        ),
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
      )

      // Bottom Convergence Arc
      val bottomPath = Path().apply {
        moveTo(w * 0.36f, h * 0.72f)
        quadraticTo(
          cx, h * 0.82f,
          w * 0.64f, h * 0.72f
        )
      }
      drawPath(
        path = bottomPath,
        brush = Brush.horizontalGradient(
          colors = listOf(KasaWarmGold, KasaWarmClay),
          startX = w * 0.36f,
          endX = w * 0.64f
        ),
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
      )

      // Central Radiant Diamond (Intelligence Beacon)
      val coreSize = w * 0.14f * pulseScale
      val diamondPath = Path().apply {
        moveTo(cx, cy - coreSize)
        lineTo(cx + coreSize * 0.8f, cy)
        lineTo(cx, cy + coreSize)
        lineTo(cx - coreSize * 0.8f, cy)
        close()
      }
      drawPath(
        path = diamondPath,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFFFFF3D6), KasaWarmGold, KasaWarmClay),
          start = Offset(cx - coreSize, cy - coreSize),
          end = Offset(cx + coreSize, cy + coreSize)
        )
      )

      // Central luminous center point
      drawCircle(
        color = Color(0xFFFFFDF5),
        radius = coreSize * 0.28f,
        center = Offset(cx, cy)
      )
    }
  }

  if (withContainer) {
    Surface(
      modifier = modifier
        .size(size * 1.35f)
        .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = KasaWarmGold.copy(alpha = 0.2f)),
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        KasaWarmGold.copy(alpha = 0.25f)
      )
    ) {
      Box(contentAlignment = Alignment.Center) {
        content()
      }
    }
  } else {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      content()
    }
  }
}

/**
 * Full KASA AI Brand Header with Emblem, Title and Tagline.
 */
@Composable
fun KasaBrandHeader(
  modifier: Modifier = Modifier,
  logoSize: Dp = 64.dp,
  showTagline: Boolean = true,
  isAnimated: Boolean = true,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    KasaLogo(
      size = logoSize,
      withContainer = true,
      isAnimated = isAnimated
    )
    Spacer(modifier = Modifier.height(14.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = "KASA",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.width(6.dp))
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = KasaWarmGold.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, KasaWarmGold.copy(alpha = 0.4f))
      ) {
        Text(
          text = "AI",
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          ),
          color = KasaWarmGold
        )
      }
    }
    if (showTagline) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Your intelligent companion, built with Ghana in mind.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
