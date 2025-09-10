package com.restaurantapp.di

import android.content.Context
import com.restaurantapp.data.network.ApiService
import com.restaurantapp.data.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideAuthInterceptor(@ApplicationContext context: Context): AuthInterceptor {
        return AuthInterceptor(context)
    }
    
    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return com.restaurantapp.data.network.NetworkModule.getApiService()
    }
}