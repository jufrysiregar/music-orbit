package com.musicorbit.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.musicorbit.db.MusicOrbitDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(MusicOrbitDatabase.Schema, "musicorbit.db")
}
