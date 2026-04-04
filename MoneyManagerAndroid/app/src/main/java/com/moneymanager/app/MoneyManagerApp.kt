package com.moneymanager.app

import android.app.Application
import com.google.firebase.FirebaseApp

class MoneyManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
