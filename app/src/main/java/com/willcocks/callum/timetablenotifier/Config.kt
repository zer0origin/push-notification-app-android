package com.willcocks.callum.timetablenotifier

import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.HashMap

class Config(val configFile: File) {
    private var contentMap = HashMap<String, String>()

    fun loadOrCreateConfig(contentToWriteToFile: () -> Unit) {
        if (configFile.exists()) {
            loadContentsFromFile()
            return
        }
        contentToWriteToFile()
        writeContentsToDisk()
    }

    fun loadOrCreateConfig() {
        if (configFile.exists()) {
            loadContentsFromFile()
            return
        }

        throw Exception("Expected config file to exist but it does not!")
    }

    fun loadContentsFromFile() {
        val temp = HashMap<String, String>()
        val p = FileReader(configFile)
        p.forEachLine { line ->
            val keypair = line.split(":")

            if (!keypair[0].trim().isEmpty()) {
                temp.put(keypair[0].trim(), keypair[1].trim())
            }

            this.contentMap = temp
        }
    }

    fun setContent(key: String, value: String) {
        contentMap.put(key, value)
    }

    fun getContent(key: String): String? {
        return contentMap.get(key)
    }

    fun writeContentsToDisk() {
        val builder = StringBuilder()
        contentMap.forEach { (key, value) ->
            builder.append("$key: $value\n")
        }

        val w = FileWriter(configFile, false)
        w.use { writer ->
            writer.write(builder.toString())
            writer.flush()
        }
    }
}