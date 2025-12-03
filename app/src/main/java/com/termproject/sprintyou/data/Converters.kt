package com.termproject.sprintyou.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: MainGoalStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): MainGoalStatus = MainGoalStatus.valueOf(value)
}