package com.example.zhivoy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.zhivoy.data.dao.AchievementDao
import com.example.zhivoy.data.dao.BookDao
import com.example.zhivoy.data.dao.FamilyDao
import com.example.zhivoy.data.dao.FoodDao
import com.example.zhivoy.data.dao.ProfileDao
import com.example.zhivoy.data.dao.SmokeDao
import com.example.zhivoy.data.dao.StepCounterStateDao
import com.example.zhivoy.data.dao.StepsDao
import com.example.zhivoy.data.dao.TrainingDao
import com.example.zhivoy.data.dao.TrainingPlanDao
import com.example.zhivoy.data.dao.TrainingTemplateDao
import com.example.zhivoy.data.dao.TrainingWeekGoalDao
import com.example.zhivoy.data.dao.UserDao
import com.example.zhivoy.data.dao.UserSettingsDao
import com.example.zhivoy.data.dao.WaterDao
import com.example.zhivoy.data.dao.WeightDao
import com.example.zhivoy.data.dao.XpDao
import com.example.zhivoy.data.entities.AchievementEntity
import com.example.zhivoy.data.entities.BookEntryEntity
import com.example.zhivoy.data.entities.FamilyEntity
import com.example.zhivoy.data.entities.FamilyMemberEntity
import com.example.zhivoy.data.entities.FoodEntryEntity
import com.example.zhivoy.data.entities.ProfileEntity
import com.example.zhivoy.data.entities.SmokeStatusEntity
import com.example.zhivoy.data.entities.StepCounterStateEntity
import com.example.zhivoy.data.entities.StepEntryEntity
import com.example.zhivoy.data.entities.TrainingEntity
import com.example.zhivoy.data.entities.TrainingPlanEntity
import com.example.zhivoy.data.entities.TrainingTemplateEntity
import com.example.zhivoy.data.entities.TrainingWeekGoalEntity
import com.example.zhivoy.data.entities.UserEntity
import com.example.zhivoy.data.entities.UserSettingsEntity
import com.example.zhivoy.data.entities.WaterEntryEntity
import com.example.zhivoy.data.entities.WeightEntryEntity
import com.example.zhivoy.data.entities.XpEventEntity

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
    version = 13,
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

