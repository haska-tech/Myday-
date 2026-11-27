package com.haskatech.myday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#7C8A9A",
    val iconKey: String = "folder",
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
