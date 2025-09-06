package com.example.myapplication.data

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class TaskStorageWithKey : RealmObject() {
    @PrimaryKey
    var id: Long = 0
    var imageTitles: RealmList<ImageTitlesStorageWithKey> = RealmList()
}