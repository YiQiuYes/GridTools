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
import androidx.compose.material.icons.filled.Schedule
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** What a [DateTimeField] edits: a plain date or a time of day. */
enum class DateTimeMode { DATE, TIME }

/**
 * A tap-to-edit date or time field.
 *
 * [DateTimeMode.DATE] opens the Material date picker and writes `yyyy-MM-dd`;
 * [DateTimeMode.TIME] opens the time picker and writes `HH:mm`. The field
 * itself never scrolls horizontally; long values wrap into a second line.
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
    mode: DateTimeMode = DateTimeMode.DATE,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pattern = if (mode == DateTimeMode.DATE) DATE_PATTERN else TIME_PATTERN
    val parsed = remember(value, mode) { parseValue(value, pattern) }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .clickable { showPicker = true }
            .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (mode == DateTimeMode.DATE) {
                Icons.Filled.EditCalendar
            } else {
                Icons.Filled.Schedule
            },
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

    when (mode) {
        DateTimeMode.DATE -> if (showPicker) {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = parsed?.let(::toUtcDateMillis)
                    ?: toUtcDateMillis(Calendar.getInstance()),
            )
            DatePickerDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onValueChange(
                                SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
                                    .format(Date(dateState.selectedDateMillis ?: System.currentTimeMillis())),
                            )
                            showPicker = false
                        },
                    ) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = dateState)
            }
        }

        DateTimeMode.TIME -> if (showPicker) {
            val timeState = rememberTimePickerState(
                initialHour = parsed?.get(Calendar.HOUR_OF_DAY)
                    ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                initialMinute = parsed?.get(Calendar.MINUTE)
                    ?: Calendar.getInstance().get(Calendar.MINUTE),
                is24Hour = true,
            )
            BasicAlertDialog(onDismissRequest = { showPicker = false }) {
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
                            TextButton(onClick = { showPicker = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            TextButton(
                                onClick = {
                                    onValueChange(
                                        String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            timeState.hour,
                                            timeState.minute,
                                        ),
                                    )
                                    showPicker = false
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
}

/** Parses a field value back into a calendar, or null when blank/unparsable. */
private fun parseValue(value: String, pattern: String): Calendar? {
    if (value.isBlank()) return null
    return runCatching {
        SimpleDateFormat(pattern, Locale.getDefault()).parse(value)?.let { date ->
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

/** `yyyy-MM-dd` in the device locale, used for the record header date. */
internal fun currentDate(): String =
    SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date())

internal const val DATE_PATTERN = "yyyy-MM-dd"
internal const val TIME_PATTERN = "HH:mm"
