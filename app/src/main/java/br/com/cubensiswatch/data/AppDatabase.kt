package br.com.cubensiswatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Cultivation::class, Culture::class, Lineage::class, CulturePhoto::class, CultivationEvent::class, Photo::class, Incubator::class, IncubatorReading::class, Material::class, StockMovement::class, ShoppingItem::class, Reminder::class], version = 6, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cultivationDao(): CultivationDao
    abstract fun eventDao(): CultivationEventDao
    abstract fun photoDao(): PhotoDao
    abstract fun lineageDao(): LineageDao
    abstract fun cultureDao(): CultureDao
    abstract fun culturePhotoDao(): CulturePhotoDao
    abstract fun incubatorDao(): IncubatorDao
    abstract fun materialDao(): MaterialDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cubensis_watch.db",
            ).addMigrations(*MIGRATIONS).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cultivations ADD COLUMN variety TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN lineage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN method TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN origin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN incubator TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN completedAt INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS cultivation_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cultivationId INTEGER NOT NULL, type TEXT NOT NULL, description TEXT NOT NULL, occurredAt INTEGER NOT NULL, temperature REAL, humidity REAL, stage TEXT, FOREIGN KEY(cultivationId) REFERENCES cultivations(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultivation_events_cultivationId ON cultivation_events(cultivationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultivation_events_occurredAt ON cultivation_events(occurredAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cultivationId INTEGER NOT NULL, eventId INTEGER, localPath TEXT NOT NULL, caption TEXT NOT NULL, capturedAt INTEGER NOT NULL, FOREIGN KEY(cultivationId) REFERENCES cultivations(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(eventId) REFERENCES cultivation_events(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_cultivationId ON photos(cultivationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_eventId ON photos(eventId)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS lineages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, code TEXT NOT NULL, name TEXT NOT NULL, species TEXT NOT NULL, variety TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE cultures ADD COLUMN species TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultures ADD COLUMN variety TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultures ADD COLUMN lineageId INTEGER REFERENCES lineages(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("ALTER TABLE cultures ADD COLUMN parentCultureId INTEGER REFERENCES cultures(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("ALTER TABLE cultures ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cultures ADD COLUMN origin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultures ADD COLUMN location TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultures ADD COLUMN incubator TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cultures ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultures_lineageId ON cultures(lineageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultures_parentCultureId ON cultures(parentCultureId)")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN lineageId INTEGER REFERENCES lineages(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN sourceCultureId INTEGER REFERENCES cultures(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultivations_lineageId ON cultivations(lineageId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultivations_sourceCultureId ON cultivations(sourceCultureId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS culture_photos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cultureId INTEGER NOT NULL, localPath TEXT NOT NULL, caption TEXT NOT NULL, capturedAt INTEGER NOT NULL, FOREIGN KEY(cultureId) REFERENCES cultures(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_culture_photos_cultureId ON culture_photos(cultureId)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS incubators (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, code TEXT NOT NULL, location TEXT NOT NULL, description TEXT NOT NULL, temperature REAL, humidity REAL, notes TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS incubator_readings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, incubatorId INTEGER NOT NULL, temperature REAL, humidity REAL, notes TEXT NOT NULL, recordedAt INTEGER NOT NULL, FOREIGN KEY(incubatorId) REFERENCES incubators(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incubator_readings_incubatorId ON incubator_readings(incubatorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incubator_readings_recordedAt ON incubator_readings(recordedAt)")
                db.execSQL("ALTER TABLE cultivations ADD COLUMN incubatorId INTEGER REFERENCES incubators(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultivations_incubatorId ON cultivations(incubatorId)")
                db.execSQL("ALTER TABLE cultures ADD COLUMN incubatorId INTEGER REFERENCES incubators(id) ON UPDATE NO ACTION ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cultures_incubatorId ON cultures(incubatorId)")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS materials (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, minimum REAL NOT NULL, ideal REAL NOT NULL, location TEXT NOT NULL, notes TEXT NOT NULL, lastPurchaseAt INTEGER, price REAL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, materialId INTEGER NOT NULL, type TEXT NOT NULL, quantity REAL NOT NULL, reason TEXT NOT NULL, cultivationId INTEGER, notes TEXT NOT NULL, occurredAt INTEGER NOT NULL, FOREIGN KEY(materialId) REFERENCES materials(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(cultivationId) REFERENCES cultivations(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_materialId ON stock_movements(materialId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_cultivationId ON stock_movements(cultivationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_occurredAt ON stock_movements(occurredAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS shopping_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, materialId INTEGER, checked INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(materialId) REFERENCES materials(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_items_materialId ON shopping_items(materialId)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, dueAt INTEGER NOT NULL, repeatIntervalMinutes INTEGER, enabled INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dueAt ON reminders(dueAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_enabled ON reminders(enabled)")
            }
        }
        val MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    }
}
