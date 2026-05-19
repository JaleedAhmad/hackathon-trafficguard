package com.traffic_guard.ai.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReportDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "trafficguard_reports.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_REPORTS = "reports"
        const val COLUMN_ID = "id"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_SEVERITY = "severity"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_VOICE_PATH = "voice_file_path"
        const val COLUMN_IMAGES_JSON = "image_uris_json"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_IS_SYNCED = "is_synced"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_REPORTS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_SEVERITY TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_VOICE_PATH TEXT,
                $COLUMN_IMAGES_JSON TEXT,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_IS_SYNCED INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
        onCreate(db)
    }

    fun insertReport(report: ReportQueueEntity): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, report.id)
            put(COLUMN_CATEGORY, report.category)
            put(COLUMN_SEVERITY, report.severity)
            put(COLUMN_LATITUDE, report.latitude)
            put(COLUMN_LONGITUDE, report.longitude)
            put(COLUMN_DESCRIPTION, report.description)
            put(COLUMN_VOICE_PATH, report.voiceFilePath)
            put(COLUMN_IMAGES_JSON, report.imageUrisJson)
            put(COLUMN_TIMESTAMP, report.timestamp)
            put(COLUMN_IS_SYNCED, if (report.isSynced) 1 else 0)
        }
        val result = db.insert(TABLE_REPORTS, null, values)
        return result != -1L
    }

    fun getAllQueuedReports(): List<ReportQueueEntity> {
        val list = mutableListOf<ReportQueueEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_REPORTS WHERE $COLUMN_IS_SYNCED = 0", null)
        if (cursor.moveToFirst()) {
            do {
                val report = ReportQueueEntity(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    severity = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SEVERITY)),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    voiceFilePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOICE_PATH)),
                    imageUrisJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGES_JSON)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED)) == 1
                )
                list.add(report)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun markReportSynced(id: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_SYNCED, 1)
        }
        db.update(TABLE_REPORTS, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun deleteReport(id: String) {
        val db = writableDatabase
        db.delete(TABLE_REPORTS, "$COLUMN_ID = ?", arrayOf(id))
    }
}
