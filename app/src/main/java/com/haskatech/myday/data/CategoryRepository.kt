package com.haskatech.myday.data

import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()

    suspend fun create(name: String, colorHex: String, iconKey: String = "folder"): Long? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 32 || !colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return null
        val id = dao.insert(CategoryEntity(name = clean, colorHex = colorHex.uppercase(), iconKey = iconKey))
        return id.takeIf { it > 0 }
    }

    suspend fun rename(id: Long, name: String, colorHex: String, iconKey: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 32 || !colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return false
        val category = dao.getById(id) ?: return false
        if (category.isDefault) return false
        return dao.update(id, clean, colorHex.uppercase(), iconKey) == 1
    }

    suspend fun delete(id: Long): Boolean {
        val category = dao.getById(id) ?: return false
        if (category.isDefault) return false
        return dao.deleteAndUncategorize(id)
    }
}
