package com.restaurantapp

import android.app.Application
import com.restaurantapp.data.network.NetworkModule
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RestaurantApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // NetworkModule 초기화
        NetworkModule.initialize(this)
    }
}