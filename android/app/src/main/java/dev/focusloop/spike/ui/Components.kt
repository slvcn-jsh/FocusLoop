package dev.focusloop.spike.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.focusloop.spike.domain.UsageDay
import dev.focusloop.spike.platform.AppIdentity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LoopMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp)) {
        val stroke = 3.5.dp.toPx()
        drawArc(Teal.copy(alpha = .28f), -90f, 360f, false, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
        drawArc(Teal, -70f, 285f, false, style = Stroke(stroke), topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke))
    }
}

@Composable
fun Page(tag: String, content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag(tag), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
fun Heading(title: String, subtitle: String? = null, eyebrow: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        eyebrow?.let { Text(it.uppercase(), color = Teal, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun LoopCard(modifier: Modifier = Modifier, tint: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = tint,
        border = BorderStroke(1.dp, if (tint == Mint) Color(0xFFCBE5E5) else Border), tonalElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Pill(text: String, tint: Color = Mint) {
    Surface(color = tint, shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = Teal)
    }
}

@Composable
fun Note(text: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    Surface(modifier.fillMaxWidth(), color = if (warning) Color(0xFFFFF6E4) else BlueWash, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Info, null, Modifier.size(20.dp), tint = if (warning) Color(0xFF795817) else Teal)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun Primary(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text(label) }
}

@Composable
fun Secondary(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border)) { Text(label) }
}

@Composable
fun LinkRow(title: String, description: String? = null, icon: @Composable (() -> Unit)? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        icon?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            description?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Teal)
    }
}

@Composable
fun AppAvatar(packageName: String, identity: AppIdentity?, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Mint), contentAlignment = Alignment.Center) {
        if (identity?.icon != null) Image(identity.icon.asImageBitmap(), null, Modifier.size(30.dp))
        else Text((identity?.name ?: packageName).take(1).uppercase(), color = Teal, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ApplicationRow(packageName: String, identity: AppIdentity?, ms: Long, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppAvatar(packageName, identity)
        Column(Modifier.weight(1f)) {
            Text(identity?.name ?: packageName, style = MaterialTheme.typography.titleSmall)
            Text("App foreground · Android buckets", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Text(shortDuration(ms), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun AdaptivePanels(left: @Composable () -> Unit, right: @Composable () -> Unit, leftBelow: @Composable () -> Unit = {}) {
    BoxWithConstraints {
        if (maxWidth >= 600.dp && LocalDensity.current.fontScale <= 1.3f) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { left(); leftBelow() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { right() }
        } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { left(); right(); leftBelow() }
    }
}

@Composable
fun UsageChart(days: List<UsageDay>) {
    val visible = days.takeLast(14)
    if (visible.isEmpty() || visible.all { it.foregroundMs == null }) {
        Text("No dated buckets available. Missing records are not zero usage.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        return
    }
    val description = visible.joinToString("; ") { "${it.date}: ${it.foregroundMs?.let(::shortDuration) ?: "no record"}" }
    val maximum = visible.mapNotNull { it.foregroundMs }.maxOrNull()?.coerceAtLeast(1) ?: 1
    Canvas(Modifier.fillMaxWidth().height(128.dp).semantics { contentDescription = description }) {
        val step = size.width / visible.size
        visible.forEachIndexed { index, day ->
            val x = step * index + step * .2f
            if (day.foregroundMs == null) drawLine(Border, Offset(x, size.height - 3), Offset(x + step * .6f, size.height - 3), 3.dp.toPx())
            else {
                val h = (day.foregroundMs.toDouble() / maximum * (size.height - 8)).toFloat().coerceAtLeast(2f)
                drawRoundRect(if (index == visible.lastIndex) Teal else Color(0xFFBBD9DE), Offset(x, size.height - h), Size(step * .6f, h), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(visible.first().date.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(visible.last().date.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.bodySmall, color = Muted)
    }
    Text("Returned daily buckets · gaps mean no record${if (days.size > 14) " · last 14 dates shown" else ""}", style = MaterialTheme.typography.bodySmall, color = Muted)
}

fun shortDuration(ms: Long): String {
    if (ms < 60_000) return "${ms.coerceAtLeast(0) / 1000}s"
    val minutes = ms / 60_000
    return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
}
fun timerDuration(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return if (s < 3600) "%02d:%02d".format(s / 60, s % 60) else "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}
fun localDateTime(ms: Long): String = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))
