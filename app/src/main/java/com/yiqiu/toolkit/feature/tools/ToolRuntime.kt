package com.yiqiu.toolkit.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yiqiu.toolkit.R
import com.yiqiu.toolkit.data.model.ToolIds
import com.yiqiu.toolkit.feature.switchrecord.SwitchRecordScreen

/**
 * Entry point for a tool's working surface.
 *
 * The catalogue stays data-driven: a tool advertises an id, and this dispatcher
 * maps that id to the composable that implements it. Adding a tool means adding
 * one branch here plus one entry in the repository.
 */
@Composable
fun ToolRuntimeScreen(
    toolId: String,
    recordId: String?,
    onBack: () -> Unit,
) {
    when (toolId) {
        ToolIds.SwitchRecord -> SwitchRecordScreen(onBack = onBack, recordId = recordId)
        else -> UnimplementedToolScreen(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnimplementedToolScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tools_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.tools_runtime_missing),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
