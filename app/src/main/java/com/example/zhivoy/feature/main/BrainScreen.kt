package com.example.zhivoy.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.data.entities.BookEntryEntity
import com.example.zhivoy.data.entities.XpEventEntity
import com.example.zhivoy.feature.main.brain.AddBookDialog
import com.example.zhivoy.feature.main.brain.UpdateProgressDialog
import com.example.zhivoy.ui.components.ModernButton
import com.example.zhivoy.ui.components.ModernCard
import com.example.zhivoy.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BrainScreen() {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    val scope = rememberCoroutineScope()

    val books by (if (userId != null) db.bookDao().observeAll(userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var updatingBook by remember { mutableStateOf<BookEntryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Мозг",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        ModernCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Тренировка ума",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Читайте книги и получайте XP за прогресс",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Моя библиотека",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить")
            }
        }

        if (books.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Пока нет добавленных книг",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(books) { book ->
                    val progress = if (book.totalPages > 0) book.pagesRead.toFloat() / book.totalPages else 0f
                    ModernCard(onClick = { updatingBook = book }) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!book.author.isNullOrBlank()) {
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            db.bookDao().deleteById(book.id)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${book.pagesRead} из ${book.totalPages} стр.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBookDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, author, totalPages ->
                if (userId != null) {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.bookDao().insert(
                                BookEntryEntity(
                                    userId = userId,
                                    title = title,
                                    author = author,
                                    totalPages = totalPages,
                                    pagesRead = 0,
                                    createdAtEpochMs = System.currentTimeMillis()
                                )
                            )
                            // Award initial XP for starting a book
                            db.xpDao().insert(
                                XpEventEntity(
                                    userId = userId,
                                    dateEpochDay = DateTime.epochDayNow(),
                                    type = "book_start",
                                    points = 10,
                                    note = "New book: $title",
                                    createdAtEpochMs = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    showAddDialog = false
                }
            }
        )
    }

    if (updatingBook != null) {
        UpdateProgressDialog(
            currentPages = updatingBook!!.pagesRead,
            totalPages = updatingBook!!.totalPages,
            onDismiss = { updatingBook = null },
            onUpdate = { pagesRead ->
                if (userId != null) {
                    scope.launch {
                        val diff = pagesRead - updatingBook!!.pagesRead
                        withContext(Dispatchers.IO) {
                            db.bookDao().updateProgress(updatingBook!!.id, pagesRead)
                            if (diff > 0) {
                                // Award XP based on pages read (1 XP per 5 pages, example)
                                val xp = (diff / 5).coerceAtLeast(1)
                                db.xpDao().insert(
                                    XpEventEntity(
                                        userId = userId,
                                        dateEpochDay = DateTime.epochDayNow(),
                                        type = "book_progress",
                                        points = xp,
                                        note = "Read $diff pages in ${updatingBook!!.title}",
                                        createdAtEpochMs = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        updatingBook = null
                    }
                }
            }
        )
    }
}
