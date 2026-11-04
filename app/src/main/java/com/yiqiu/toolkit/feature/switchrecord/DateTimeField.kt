package com.yiqiu.toolkit.feature.switchrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yiqiu.toolkit.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * A tap-to-edit date + time field.
 *
 * Tapping opens the Material date picker, then the time picker, and writes the
 * combined `yyyy-MM-dd HH:mm` value back. The field itself never scrolls
 * horizontally; long values wrap into a second line instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    compact: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    val parsed = remember(value) { parseDateTime(value) }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .clickable { showDatePicker = true }
            .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.EditCalendar,
            contentDescription = null,
            modifier = Modifier.size(if (compact) 16.dp else 18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value.ifBlank { stringResource(R.string.switch_record_time_unset) },
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = parsed?.let(::toUtcDateMillis)
                ?: toUtcDateMillis(Calendar.getInstance()),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDateMillis = dateState.selectedDateMillis
                            ?: toUtcDateMillis(Calendar.getInstance())
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = parsed?.get(Calendar.HOUR_OF_DAY) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            initialMinute = parsed?.get(Calendar.MINUTE) ?: Calendar.getInstance().get(Calendar.MINUTE),
            is24Hour = true,
        )
        BasicAlertDialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.switch_record_pick_time),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                onValueChange(formatDateTime(pickedDateMillis, timeState.hour, timeState.minute))
                                showTimePicker = false
                            },
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                }
            }
        }
    }
}

/** Parses a `yyyy-MM-dd HH:mm` value back into a calendar, or null when blank. */
private fun parseDateTime(value: String): Calendar? {
    if (value.isBlank()) return null
    return runCatching {
        SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).parse(value)?.let { date ->
            Calendar.getInstance().apply { time = date }
        }
    }.getOrNull()
}

/** DatePicker works in UTC midnights, so normalise the local date to one. */
private fun toUtcDateMillis(calendar: Calendar): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )
    }.timeInMillis

private fun formatDateTime(dateMillis: Long?, hour: Int, minute: Int): String {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = dateMillis ?: System.currentTimeMillis()
    }
    val local = Calendar.getInstance().apply {
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            hour,
            minute,
            0,
        )
        set(Calendar.MILLISECOND, 0)
    }
    return SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(local.time)
}
