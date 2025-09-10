package com.restaurantapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.restaurantapp.data.model.Restaurant

@Database(
    entities = [Restaurant::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun restaurantDao(): RestaurantDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "restaurant_database"
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2) // 향후 마이그레이션 대비
                    .fallbackToDestructiveMigration() // 개발 단계에서만 사용
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        // Restaurant 테이블에 mainCategory, subCategory 컬럼 추가
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // mainCategory 컬럼 추가 (기본값: category 값 사용)
                database.execSQL("ALTER TABLE restaurants ADD COLUMN mainCategory TEXT NOT NULL DEFAULT ''")
                
                // subCategory 컬럼 추가 (nullable)
                database.execSQL("ALTER TABLE restaurants ADD COLUMN subCategory TEXT")
                
                // 기존 category 값을 mainCategory로 복사
                database.execSQL("UPDATE restaurants SET mainCategory = category WHERE mainCategory = ''")
            }
        }
        
        // 테스트용 메모리 데이터베이스
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
        
        // 데이터베이스 인스턴스 강제 삭제 (테스트용)
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}