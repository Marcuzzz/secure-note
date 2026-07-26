package com.example.securenote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.securenote.data.Note
import com.example.securenote.ui.vm.NoteListViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    onGenerator: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    vm: NoteListViewModel = viewModel(factory = NoteListViewModel.Factory),
) {
    val notes by vm.notes.collectAsState()
    val query by vm.query.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureNote") },
                actions = {
                    IconButton(onClick = onGenerator) {
                        Icon(Icons.Filled.Casino, contentDescription = "Password generator")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { vm.lock(); onLock() }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No notes yet. Tap + to create one.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(notes, key = { it.id }) { NoteRow(it, onClick = { onOpen(it.id) }) }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            if (note.username.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.username, style = MaterialTheme.typography.bodySmall)
            }
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.body.take(120),
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(note.updatedAt)),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
