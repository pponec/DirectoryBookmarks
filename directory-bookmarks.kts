#!/usr/bin/env kscript
package net.ponec.kotlin.utils.script

import java.io.File
import java.util.regex.Pattern
import kotlin.system.exitProcess

val homePage = "https://github.com/pponec/DirectoryBookmarks"
val appName = "directory-bookmarks.kts"
val appVersion = "1.1"
val storeName = ".directory-bookmarks.csv"
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
            val msg = args.sliceArray(3 until args.size)
            save(args[1], args[2], *msg)
        }
        "l" -> printDirectories()
        "i" -> printInstall()
        else -> printHelpAndExit()
    }
}

fun printHelpAndExit() {
    var bashrc = "~/.bashrc"
    println("Script '$appName' v$appVersion ($homePage)")
    println("Usage version: $appName [rwl] bookmark directory optionalComment")
    println("Integrate it to Linux Ubuntu: $appName i >> $bashrc && . $bashrc")
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
        "." -> return key
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
    val commentmMatcher = Pattern.compile("\\s+$comment\\s").matcher(dir)
    return if (commentmMatcher.find()) {
        dir.substring(0, commentmMatcher.start())
    } else {
        dir
    }
}

fun save(key: String, dir: String, vararg comments: String) {
    require(!key.contains(separator), { "the key contains a tab" })
    val extendedKey = key + separator
    val tempFile = getStoreFileTemplate()
    val storeFile = getStoreFile()
    tempFile.bufferedWriter().use { writer ->
        writer.write(header)
        writer.write(newLine)
        writer.write("$key$separator$dir")
        if (!comments.isEmpty()) {
            writer.write("$separator#")
            comments.forEach { writer.append(" $it") }
        }
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
        cdf() { cd "${'$'}($appName r ${'$'}1)"; }
        sdf() { $appName w "${'$'}1" "${'$'}PWD" ${'$'}{@:2}; }
        ldf() { $appName l; }
    """.trimIndent()
    println(msg)
}
