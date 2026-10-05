package com.musicorbit.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * expect: each platform provides the correct SqlDriver implementation.
 * Android: AndroidSqliteDriver
 * iOS:     NativeSqliteDriver
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
