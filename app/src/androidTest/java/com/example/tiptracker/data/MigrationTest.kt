package com.example.tiptracker.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the exported schemas in app/schemas against the auto-migrations.
 * Instrumented: needs a device or emulator (`./gradlew :app:connectedDebugAndroidTest`).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TipTrackerDatabase::class.java
    )

    @Test
    fun migrate3To4_keepsExistingLogsAndLeavesPlaceColumnsNull() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO log (id, bill, tip_percent, total, party_size, restaurant_name, " +
                        "address, review, rating, date) VALUES " +
                        "(1, 50.0, 18.0, 59.0, 2, 'The Pearl', '1 Main St', 'Great', 8.5, '2026-03-21')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query(
            "SELECT restaurant_name, address, review, place_id, latitude, longitude " +
                    "FROM log WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("The Pearl", cursor.getString(0))
            assertEquals("1 Main St", cursor.getString(1))
            assertEquals("Great", cursor.getString(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertTrue(cursor.isNull(5))
        }
    }

    @Test
    fun migrate3To4_createsPlaceIdIndex() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_log_place_id'"
        ).use { cursor ->
            assertEquals(1, cursor.count)
        }
    }

    @Test
    fun migrate3To4_newColumnsAcceptPlaceData() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)
        db.execSQL(
            "INSERT INTO log (bill, tip_percent, total, party_size, restaurant_name, address, " +
                    "review, rating, date, place_id, latitude, longitude) VALUES " +
                    "(10.0, 15.0, 11.5, 1, 'Juniper', 'Elm St', '', 7.0, '2026-04-01', " +
                    "'place-123', 45.5, -122.6)"
        )

        db.query("SELECT place_id, latitude, longitude FROM log WHERE restaurant_name = 'Juniper'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("place-123", cursor.getString(0))
                assertEquals(45.5, cursor.getDouble(1), 0.0)
                assertEquals(-122.6, cursor.getDouble(2), 0.0)
            }
    }

    @Test
    fun migrateFromVersion1ToLatest_keepsData() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO log (id, bill, tip_percent, total, party_size, restaurant_name, " +
                        "review, rating, date) VALUES " +
                        "(1, 40.0, 20.0, 48.0, 4, 'Harbor Kitchen', 'Nice', 9.0, '2025-12-01')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        db.query("SELECT restaurant_name, address, place_id FROM log WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Harbor Kitchen", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }
    }

    @Test
    fun roomOpensMigratedDatabaseWithoutSchemaErrors() {
        helper.createDatabase(TEST_DB, 3).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, TipTrackerDatabase::class.java, TEST_DB)
            .build()
        try {
            // Opening validates the on-disk schema against the current entities
            database.openHelper.writableDatabase
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
