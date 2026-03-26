package com.example.bookmymovie.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun DonutChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    innerRadiusFraction: Float = 0.7f,
    strokeWidth: Dp = 16.dp,
    centerLabel: String = "",
    centerValue: String = ""
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    var startAngle = -90f

    val animateFloat = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val radius = minOf(canvasSize.width, canvasSize.height) / 2
            val rectSize = Size(radius * 2, radius * 2)
            val topLeft = Offset((canvasSize.width - rectSize.width) / 2, (canvasSize.height - rectSize.height) / 2)

            data.forEach { item ->
                val sweepAngle = (item.value / total) * 360f
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animateFloat.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = rectSize,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerLabel.isNotEmpty()) {
                Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (centerValue.isNotEmpty()) {
                Text(centerValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun Legend(data: List<ChartData>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(item.color, MaterialTheme.shapes.extraSmall))
                Spacer(Modifier.width(8.dp))
                Text(item.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(item.value.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
