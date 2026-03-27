package com.smarttravel.app.di

import android.content.Context
import androidx.room.Room
import com.smarttravel.app.data.local.SmartTravelDatabase
import com.smarttravel.app.data.remote.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── OkHttp ───────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor { chain ->
                // Set User-Agent required by Nominatim policy
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SmartTravelApp/1.0 Android")
                    .build()
                chain.proceed(request)
            }
            .build()

    // ── Nominatim ────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApi =
        retrofit.create(NominatimApi::class.java)

    // ── OSRM ─────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("osrm")
    fun provideOsrmRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideOsrmApi(@Named("osrm") retrofit: Retrofit): OsrmApi =
        retrofit.create(OsrmApi::class.java)

    // ── Overpass ─────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideOverpassApi(@Named("overpass") retrofit: Retrofit): OverpassApi =
        retrofit.create(OverpassApi::class.java)

    // ── Room Database ────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartTravelDatabase =
        Room.databaseBuilder(context, SmartTravelDatabase::class.java, "smart_travel.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTravelSessionDao(db: SmartTravelDatabase) = db.travelSessionDao()
    @Provides fun provideLocationPointDao(db: SmartTravelDatabase) = db.locationPointDao()
    @Provides fun provideSavedPlaceDao(db: SmartTravelDatabase) = db.savedPlaceDao()
    @Provides fun provideDailySummaryDao(db: SmartTravelDatabase) = db.dailySummaryDao()
}
