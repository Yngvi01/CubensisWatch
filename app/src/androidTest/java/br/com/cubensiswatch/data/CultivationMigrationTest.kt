package br.com.cubensiswatch.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CultivationMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationFromPhaseOneKeepsExistingCultivation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO cultivations (code, name, species, status, startedAt) VALUES ('CT-001', 'Cultivo legado', 'Espécie X', 'Incubação', 1000)")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT code, name, method FROM cultivations WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CT-001", cursor.getString(0))
                assertEquals("Cultivo legado", cursor.getString(1))
                assertEquals("", cursor.getString(2))
            }
            close()
        }
        context.deleteDatabase(name)
    }

    @Test
    fun migrationToLineagesKeepsPhaseTwoCultivationAndCulture() {
        val name = "phase-two-migration-test.db"
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO cultures (code, name, type, status) VALUES ('AG-001', 'Ágar legado', 'Ágar', 'Ativo')")
            execSQL("INSERT INTO cultivations (code, name, species, status, startedAt) VALUES ('CT-001', 'Cultivo legado', 'Espécie X', 'Incubação', 1000)")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT code, name, lineageId, parentCultureId FROM cultures WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("AG-001", cursor.getString(0))
                assertEquals("Ágar legado", cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
            }
            query("SELECT code, lineageId, sourceCultureId FROM cultivations WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CT-001", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
            close()
        }
    }

    @Test
    fun migrationToIncubatorsPreservesRecordsAndAddsAssociations() {
        val name = "phase-three-migration-test.db"
        helper.createDatabase(name, 3).apply {
            execSQL("INSERT INTO cultivations (code, name, species, variety, lineage, status, startedAt, method, origin, incubator, notes) VALUES ('CT-003', 'Cultivo preservado', '', '', '', 'Incubação', 1000, '', '', '', '')")
            execSQL("INSERT INTO cultures (code, name, type, status, species, variety, createdAt, origin, location, incubator, notes) VALUES ('AG-003', 'Ágar preservado', 'Ágar', 'Ativo', '', '', 1000, '', '', '', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 4, true, *AppDatabase.MIGRATIONS).apply {
            execSQL("INSERT INTO incubators (name, code, location, description, temperature, humidity, notes) VALUES ('Caixa 1', 'INC-001', 'Prateleira', '', 24.0, 65.0, '')")
            execSQL("UPDATE cultivations SET incubatorId = 1 WHERE id = 1")
            execSQL("UPDATE cultures SET incubatorId = 1 WHERE id = 1")
            execSQL("INSERT INTO incubator_readings (incubatorId, temperature, humidity, notes, recordedAt) VALUES (1, 23.5, 67.0, 'Medição', 2000)")
            query("SELECT code, incubatorId FROM cultivations WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CT-003", cursor.getString(0))
                assertEquals(1L, cursor.getLong(1))
            }
            query("SELECT code, incubatorId FROM cultures WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("AG-003", cursor.getString(0))
                assertEquals(1L, cursor.getLong(1))
            }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToInventoryCreatesEmptyTablesWithoutChangingCultivations() {
        val name = "phase-four-migration-test.db"
        helper.createDatabase(name, 4).apply {
            execSQL("INSERT INTO cultivations (code, name, species, variety, lineage, status, startedAt, method, origin, incubator, notes) VALUES ('CT-004', 'Cultivo existente', '', '', '', 'Incubação', 1000, '', '', '', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 5, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT code, name FROM cultivations WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CT-004", cursor.getString(0))
                assertEquals("Cultivo existente", cursor.getString(1))
            }
            query("SELECT COUNT(*) FROM materials").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToRemindersPreservesInventoryAndAddsScheduleStorage() {
        val name = "phase-five-migration-test.db"
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO materials (name, category, quantity, unit, minimum, ideal, location, notes) VALUES ('Álcool', 'Higiene', 2.0, 'L', 0.5, 3.0, '', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, *AppDatabase.MIGRATIONS).apply {
            execSQL("INSERT INTO reminders (title, description, dueAt, repeatIntervalMinutes, enabled) VALUES ('Verificar estoque', '', 1000, 1440, 1)")
            query("SELECT name, quantity FROM materials WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Álcool", cursor.getString(0))
                assertEquals(2.0, cursor.getDouble(1), 0.0)
            }
            query("SELECT title, repeatIntervalMinutes FROM reminders WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Verificar estoque", cursor.getString(0))
                assertEquals(1440L, cursor.getLong(1))
            }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToHarvestsAddsEmptyTableAndKeepsCultivation() {
        val name = "phase-six-migration.db"
        helper.createDatabase(name, 6).apply {
            execSQL("INSERT INTO cultivations (code, name, species, variety, lineage, status, startedAt, method, origin, incubator, notes) VALUES ('CT-006', 'Cultivo preservado', '', '', '', 'Frutificação', 1000, '', '', '', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 7, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT code, name FROM cultivations WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("CT-006", cursor.getString(0))
                assertEquals("Cultivo preservado", cursor.getString(1))
            }
            query("SELECT COUNT(*) FROM harvests").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToHarvestPhotosAddsLocalPhotoTable() {
        val name = "harvest-photos-migration-test.db"
        helper.createDatabase(name, 7).apply {
            execSQL("INSERT INTO cultivations (code, name, species, status, startedAt) VALUES ('CT-007', 'Cultivo', '', 'Colheita', 1000)")
            execSQL("INSERT INTO harvests (cultivationId, number, harvestedAt, quantity, unit, notes) VALUES (1, 1, 2000, 120.0, 'g', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 8, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT COUNT(*) FROM harvest_photos").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToNotesAddsSearchableNotesStorage() {
        val name = "notes-migration-test.db"
        helper.createDatabase(name, 8).apply { close() }
        helper.runMigrationsAndValidate(name, 9, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT COUNT(*) FROM notes").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToNotePhotosAddsLinkedPhotoStorage() {
        val name = "note-photos-migration-test.db"
        helper.createDatabase(name, 9).apply {
            execSQL("INSERT INTO notes (title, text, createdAt, updatedAt, tags) VALUES ('Nota', 'Conteúdo', 1000, 1000, '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 10, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT COUNT(*) FROM note_photos").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }

    @Test
    fun migrationToMaterialPhotosAddsLinkedPhotoStorage() {
        val name = "material-photos-migration-test.db"
        helper.createDatabase(name, 10).apply {
            execSQL("INSERT INTO materials (name, category, quantity, unit, minimum, ideal, location, notes) VALUES ('Álcool', 'Higiene', 2.0, 'L', 0.5, 3.0, '', '')")
            close()
        }
        helper.runMigrationsAndValidate(name, 11, true, *AppDatabase.MIGRATIONS).apply {
            query("SELECT COUNT(*) FROM material_photos").use { cursor -> check(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
            query("SELECT name, quantity FROM materials WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Álcool", cursor.getString(0))
                assertEquals(2.0, cursor.getDouble(1), 0.0)
            }
            query("PRAGMA foreign_key_check").use { cursor -> check(!cursor.moveToFirst()) }
            close()
        }
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(name)
    }
}
