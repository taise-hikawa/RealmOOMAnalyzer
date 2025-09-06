package com.example.myapplication.domain

import com.example.myapplication.data.ImageTitlesStorage
import com.example.myapplication.data.ImageTitlesStorageWithKey

data class TaskImageTitles(
    val title: String,
    val category: String,
    val portalCategory: String
) {
    constructor(storage: ImageTitlesStorage) : this(
        title = storage.title,
        category = storage.category,
        portalCategory = storage.portalCategory
    )

    constructor(storage: ImageTitlesStorageWithKey) : this(
        title = storage.title,
        category = storage.category,
        portalCategory = storage.portalCategory
    )
}