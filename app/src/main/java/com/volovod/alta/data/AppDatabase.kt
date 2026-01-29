package com.volovod.alta.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.volovod.alta.data.dao.AchievementDao
import com.volovod.alta.data.dao.BookDao
import com.volovod.alta.data.dao.FamilyDao
import com.volovod.alta.data.dao.FoodDao
import com.volovod.alta.data.dao.ProfileDao
import com.volovod.alta.data.dao.SmokeDao
import com.volovod.alta.data.dao.StepCounterStateDao
import com.volovod.alta.data.dao.StepsDao
import com.volovod.alta.data.dao.SyncQueueDao
import com.volovod.alta.data.dao.TrainingDao
import com.volovod.alta.data.dao.TrainingPlanDao
import com.volovod.alta.data.dao.TrainingTemplateDao
import com.volovod.alta.data.dao.TrainingWeekGoalDao
import com.volovod.alta.data.dao.UserDao
import com.volovod.alta.data.dao.UserSettingsDao
import com.volovod.alta.data.dao.WaterDao
import com.volovod.alta.data.dao.WeightDao
import com.volovod.alta.data.dao.XpDao
import com.volovod.alta.data.entities.AchievementEntity
import com.volovod.alta.data.entities.BookEntryEntity
import com.volovod.alta.data.entities.FamilyEntity
import com.volovod.alta.data.entities.FamilyMemberEntity
import com.volovod.alta.data.entities.FoodEntryEntity
import com.volovod.alta.data.entities.ProfileEntity
import com.volovod.alta.data.entities.SmokeStatusEntity
import com.volovod.alta.data.entities.StepCounterStateEntity
import com.volovod.alta.data.entities.StepEntryEntity
import com.volovod.alta.data.entities.SyncQueueEntity
import com.volovod.alta.data.entities.TrainingEntity
import com.volovod.alta.data.entities.TrainingPlanEntity
import com.volovod.alta.data.entities.TrainingTemplateEntity
import com.volovod.alta.data.entities.TrainingWeekGoalEntity
import com.volovod.alta.data.entities.UserEntity
import com.volovod.alta.data.entities.UserSettingsEntity
import com.volovod.alta.data.entities.WaterEntryEntity
import com.volovod.alta.data.entities.WeightEntryEntity
import com.volovod.alta.data.entities.XpEventEntity

@Database(
    entities = [
        AchievementEntity::class,
        BookEntryEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class,
        FoodEntryEntity::class,
        ProfileEntity::class,
        SmokeStatusEntity::class,
        StepCounterStateEntity::class,
        StepEntryEntity::class,
        SyncQueueEntity::class,
        TrainingEntity::class,
        TrainingPlanEntity::class,
        TrainingTemplateEntity::class,
        TrainingWeekGoalEntity::class,
        UserEntity::class,
        UserSettingsEntity::class,
        WaterEntryEntity::class,
        WeightEntryEntity::class,
        XpEventEntity::class,
    ],
    version = 15,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun bookDao(): BookDao
    abstract fun familyDao(): FamilyDao
    abstract fun foodDao(): FoodDao
    abstract fun profileDao(): ProfileDao
    abstract fun smokeDao(): SmokeDao
    abstract fun stepCounterStateDao(): StepCounterStateDao
    abstract fun stepsDao(): StepsDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun trainingDao(): TrainingDao
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun trainingTemplateDao(): TrainingTemplateDao
    abstract fun trainingWeekGoalDao(): TrainingWeekGoalDao
    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao
    abstract fun xpDao(): XpDao
}

