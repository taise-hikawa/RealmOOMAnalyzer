package com.example.myapplication.data

import io.realm.RealmObject

open class ImageTitlesStorage : RealmObject() {
    var title: String = ""
    var category: String = ""
    var portalCategory: String = ""
}