#!/usr/bin/env kscript
package net.ponec.kotlin.utils.script

import java.io.File
import kotlin.system.exitProcess

val appName = "directory-bookmarks.kts"
val storeName = ".directory-bookmarks.txt"
val separator = '\t'
val comment = '#'
val newLine = System.lineSeparator()
val header = "$comment A directory bookmarks for the '$appName' script"
val homeDir = System.getProperty("user.home")

// Uncomment it for kscript:
main(args)

fun main(args: Array<String>) {
    if (args.isEmpty()) printHelpAndExit()
    when (args[0]) {
        "r" -> {
            if (args.size < 2) printHelpAndExit()
            val dir = getDirectory(args[1], " ${args[1]} [bookmark] ")
            println(dir)
        }
        "w" -> {
            if (args.size < 3) printHelpAndExit()
            save(args[1], args[2])
        }
        "l" -> {
            printDirectories()
        }
        "i" -> {
            printInstall()
        }
        else -> {
            printHelpAndExit()
        }
    }
}

fun printHelpAndExit() {
    var bashrc = "~/.bashrc"
    println("Use: $appName [rwl] bookmark directory")
    println("  or install to Ubuntu: $appName i >> $bashrc && . $bashrc")
    exitProcess(1)
}

fun printDirectories() {
    val storeFile = getStoreFile()
    return storeFile.bufferedReader().use {
        it.lines()
            .filter { !it.startsWith(comment) }
            .sorted()
            .forEach{ println("$it") }
    }
}

fun getDirectory(key: String, defaultDir: String) : String {
    when(key) {
        "~" -> return homeDir
    }
    val extendedKey = key + separator
    val storeFile = getStoreFile()
    val dir = storeFile.bufferedReader().use {
        it.lines()
            .filter { !it.startsWith(comment) }
            .filter { it.startsWith(extendedKey) }
            .map { it.substring(extendedKey.length) }
            .findFirst()
            .orElse(defaultDir)
    }
    val commentSeparator = " # "
    val hashIndex = dir.lastIndexOf(commentSeparator)
    return if (hashIndex > 0) {
        dir.substring(hashIndex + commentSeparator.length).trim()
    } else {
        dir
    }
}

fun save(key: String, dir: String) {
    val extendedKey = key + separator
    val tempFile = getStoreFileTemplate()
    val storeFile = getStoreFile()
    tempFile.bufferedWriter().use { writer ->
        writer.write(header)
        writer.write(newLine)
        writer.write("$key$separator$dir")
        writer.write(newLine)
        storeFile.bufferedReader().use {
            it.lines()
                .filter { !it.startsWith(comment) }
                .filter { !it.startsWith(extendedKey) }
                .forEach {
                    writer.write(it)
                    writer.write(newLine)
                }
        }
    }
    tempFile.renameTo(storeFile)
}

fun getStoreFile(): File {
    val result = File(homeDir, storeName)
    if (!result.isFile) {
        result.createNewFile()
    }
    return result
}

fun getStoreFileTemplate(): File {
    return File.createTempFile("storeName", "", File(homeDir));
}

fun printInstall() {
    val msg = """
        # Shortcuts for $appName utilities:
        cdf() { b="${'$'}1" && cd "${'$'}($appName r ${'$'}b)"; }
        sdf() { $appName w "${'$'}1" "${'$'}PWD"; }
        ldf() { $appName l; }
    """.trimIndent()
    println(msg)
}
