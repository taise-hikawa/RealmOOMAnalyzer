package com.example.myapplication.domain

import com.example.myapplication.data.TaskStorage
import com.example.myapplication.data.TaskStorageWithKey

data class Task(
    val id: Long,
    val imageTitles: List<TaskImageTitles>
) {
    constructor(storage: TaskStorage) : this(
        id = storage.id,
        imageTitles = storage.imageTitles.toList().map(::TaskImageTitles)
    )

    constructor(storage: TaskStorageWithKey) : this(
        id = storage.id,
        imageTitles = storage.imageTitles.toList().map(::TaskImageTitles)
    )
}