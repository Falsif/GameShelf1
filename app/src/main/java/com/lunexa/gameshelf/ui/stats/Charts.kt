package com.lunexa.gameshelf.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunexa.gameshelf.ui.common.MonthShort

data class Slice(val label: String, val value: Int, val color: Color)

/** Кольцевая диаграмма с анимацией появления. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val strokeW = this.size.minDimension * 0.14f
            val inset = strokeW / 2
            val arcSize = Size(this.size.width - strokeW, this.size.height - strokeW)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(strokeW))
            var start = -90f
            val gap = if (slices.count { it.value > 0 } > 1) 3f else 0f
            slices.filter { it.value > 0 }.forEach { s ->
                val sweep = 360f * s.value / total * progress.value
                drawArc(
                    s.color, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false,
                    Offset(inset, inset), arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.headlineMedium)
            Text(centerLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Горизонтальные полосы (жанры, платформы, топ по времени). */
@Composable
fun HorizontalBars(
    items: List<Pair<String, Int>>,
    color: Color,
    modifier: Modifier = Modifier,
    valueText: (Int) -> String = { it.toString() }
) {
    val max = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, (label, value) ->
            val fraction by animateFloatAsState(
                targetValue = if (started) value.toFloat() / max else 0f,
                animationSpec = tween(700, delayMillis = i * 60),
                label = "bar"
            )
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(valueText(value), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 1f - i * 0.1f))
                    )
                }
            }
        }
    }
}

/** Столбики по месяцам года. */
@Composable
fun MonthBars(
    values: List<Int>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    highlightMonth: Int? = null
) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            values.forEach { v ->
                Text(
                    if (v > 0) "$v" else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Canvas(Modifier.fillMaxWidth().height(height).padding(top = 4.dp)) {
            val slot = size.width / 12f
            val barW = slot * 0.56f
            val radius = CornerRadius(barW / 2, barW / 2)
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), radius)
                if (v > 0) {
                    val h = size.height * v / max * progress.value
                    val c = if (highlightMonth == null || highlightMonth == i) color else color.copy(alpha = 0.6f)
                    drawRoundRect(c, Offset(x, size.height - h), Size(barW, h), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            MonthShort.forEachIndexed { i, m ->
                Text(
                    m,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = if (highlightMonth == i) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
