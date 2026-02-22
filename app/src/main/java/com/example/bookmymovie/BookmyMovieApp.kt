package com.example.bookmymovie

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class BookmyMovieApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable offline persistence so cinemas are readable without internet
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
