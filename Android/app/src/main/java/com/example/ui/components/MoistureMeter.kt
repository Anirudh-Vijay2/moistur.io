package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MoistureLevel
import com.example.ui.theme.MoistureDry
import com.example.ui.theme.MoistureNormal
import com.example.ui.theme.MoistureVeryDry
import com.example.ui.theme.MoistureWet

@Composable
fun MoistureMeter(
    moisturePercentage: Int,
    modifier: Modifier = Modifier
) {
    val clamped = moisturePercentage.coerceIn(0, 100)
    val level = MoistureLevel.fromPercentage(clamped)

    val animatedProgress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "moisture_progress"
    )

    val animatedDisplayValue by animateIntAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "moisture_number"
    )

    val levelColor = when (level) {
        MoistureLevel.VERY_DRY -> MoistureVeryDry
        MoistureLevel.DRY -> MoistureDry
        MoistureLevel.NORMAL -> MoistureNormal
        MoistureLevel.WET -> MoistureWet
    }

    val animatedColor by animateColorAsState(
        targetValue = levelColor,
        animationSpec = tween(durationMillis = 600),
        label = "level_color"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier.testTag("moisture_meter_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            Canvas(modifier = Modifier.size(220.dp).testTag("moisture_meter_canvas")) {
                val strokeWidth = 22.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                // 270 degree circular arc (starts from 135 deg to 405 deg)
                val startAngle = 135f
                val sweepMaxAngle = 270f

                // Background track arc
                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepMaxAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground progress arc
                val currentSweep = sweepMaxAngle * animatedProgress
                if (currentSweep > 0.1f) {
                    val brush = Brush.sweepGradient(
                        0.0f to animatedColor.copy(alpha = 0.85f),
                        0.5f to animatedColor,
                        1.0f to animatedColor,
                        center = center
                    )
                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Inside Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Water Drop Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(animatedColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Moisture indicator",
                        tint = animatedColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Percentage number
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$animatedDisplayValue",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 56.sp
                    )
                    Text(
                        text = "%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedColor,
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title and Status Label
        Text(
            text = "Soil Moisture",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Moisture Level Pill Badge
        Box(
            modifier = Modifier
                .background(
                    color = animatedColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("moisture_level_badge"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = animatedColor
            )
        }
    }
}
