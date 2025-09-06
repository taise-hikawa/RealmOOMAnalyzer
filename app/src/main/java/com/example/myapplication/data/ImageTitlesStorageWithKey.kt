package com.example.myapplication.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class ImageTitlesStorageWithKey : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var title: String = ""
    var category: String = ""
    var portalCategory: String = ""
}