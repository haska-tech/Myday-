package com.haskatech.myday.data

import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()

    suspend fun ensureDefaults() {
        val defaults = listOf(
            CategoryEntity(id = 1, name = "Personal", colorHex = "#5B8DEF", isDefault = true),
            CategoryEntity(id = 2, name = "Work", colorHex = "#7C6EE6", isDefault = true),
            CategoryEntity(id = 3, name = "Family", colorHex = "#E58A4E", isDefault = true),
            CategoryEntity(id = 4, name = "Health", colorHex = "#38A88A", isDefault = true),
            CategoryEntity(id = 5, name = "Finance", colorHex = "#C49A36", isDefault = true),
            CategoryEntity(id = 6, name = "Education", colorHex = "#B36BC7", isDefault = true)
        )
        dao.insertAll(defaults)
    }

    suspend fun create(name: String, colorHex: String, iconKey: String = "folder"): Long? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 32 || !colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return null
        val id = dao.insert(CategoryEntity(name = clean, colorHex = colorHex.uppercase(), iconKey = iconKey))
        return id.takeIf { it > 0 }
    }

    suspend fun rename(id: Long, name: String, colorHex: String, iconKey: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 32 || !colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return false
        if (dao.getById(id) == null) return false
        return dao.update(id, clean, colorHex.uppercase(), iconKey) == 1
    }

    suspend fun delete(id: Long): Boolean {
        if (dao.getById(id) == null) return false
        return dao.deleteAndUncategorize(id)
    }
}
