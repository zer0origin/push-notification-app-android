package com.willcocks.callum.timetablenotifier.models

enum class Files {
    SETTINGS_FILE_NAME {
        override fun toString(): String {
            return "settings"
        }
    };
}