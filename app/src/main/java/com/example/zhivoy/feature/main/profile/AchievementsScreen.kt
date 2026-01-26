package com.example.zhivoy.feature.main.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhivoy.LocalAppDatabase
import com.example.zhivoy.LocalSessionStore
import com.example.zhivoy.ui.components.ModernCard

data class AchievementDef(
    val code: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

val ACHIEVEMENT_DEFS = listOf(
    AchievementDef("steps_10k", "Первая десятка", "Пройти 10,000 шагов за день", Icons.Default.DirectionsWalk, Color(0xFF4CAF50)),
    AchievementDef("water_hero", "Водный герой", "Выпить 2 литра воды за день", Icons.Default.LocalDrink, Color(0xFF2196F3)),
    AchievementDef("book_worm", "Книжный червь", "Добавить свою первую книгу", Icons.Default.AutoStories, Color(0xFF9C27B0)),
    AchievementDef("smoke_free", "Чистые легкие", "Не курить более 24 часов", Icons.Default.SmokingRooms, Color(0xFFFF9800)),
    AchievementDef("plank_master", "Мастер планки", "Простоять в планке более 2 минут", Icons.Default.FitnessCenter, Color(0xFFF44336)),
    AchievementDef("early_bird", "Ранняя пташка", "Зайти в приложение до 8 утра", Icons.Default.WbSunny, Color(0xFFFFEB3B))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val db = LocalAppDatabase.current
    val sessionStore = LocalSessionStore.current
    val session by sessionStore.session.collectAsState(initial = null)
    val userId = session?.userId
    
    val userAchievements by (if (userId != null) db.achievementDao().observeAll(userId) else kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())
        val unlockedCodes = remember(userAchievements) { userAchievements.map { it.code }.toSet() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Зал славы", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Ваши достижения: ${unlockedCodes.size} / ${ACHIEVEMENT_DEFS.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ACHIEVEMENT_DEFS) { def ->
                    val isUnlocked = unlockedCodes.contains(def.code)
                    AchievementItem(def, isUnlocked)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(def: AchievementDef, isUnlocked: Boolean) {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) def.color.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = def.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isUnlocked) def.color else MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = def.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = def.description,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}












