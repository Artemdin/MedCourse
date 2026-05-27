package com.example.medcourse.ui

import android.content.Context

object NotificationStateStore {
    private const val PREFS_NAME = "med_notification_state"

    private fun key(medId: Int): String = "pending_$medId"

    fun isPending(context: Context, medId: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key(medId), false)
    }

    fun setPending(context: Context, medId: Int, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key(medId), pending).apply()
    }

    fun clear(context: Context, medId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(key(medId)).apply()
    }
}
