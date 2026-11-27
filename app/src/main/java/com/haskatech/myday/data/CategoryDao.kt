package com.haskatech.myday.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :name, colorHex = :colorHex, iconKey = :iconKey WHERE id = :id")
    suspend fun update(id: Long, name: String, colorHex: String, iconKey: String): Int

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun moveTasksToInbox(categoryId: Long): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCustom(id: Long): Int

    @Transaction
    suspend fun deleteAndUncategorize(id: Long): Boolean {
        moveTasksToInbox(id)
        return deleteCustom(id) == 1
    }

    @Query("DELETE FROM categories")
    suspend fun clearAll()

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
}
