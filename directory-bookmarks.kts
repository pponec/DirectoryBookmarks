#!/usr/bin/env kscript
package net.ponec.kotlin.utils.script1

import java.io.File
import java.util.regex.Pattern
import kotlin.system.exitProcess

/** Compile to a Java archive and run it:
 * 1. Run: `cp DirectoryBookmarks.kts DirectoryBookmarks.kt`
 * 2. Comment the last statement.
 * 3. Run: `kotlinc DirectoryBookmarks.kt -include-runtime -d DirectoryBookmarks.jar`
 * 4. Run: `java -jar DirectoryBookmarks.jar [parameter(s)]`
 */
object MainSingleton {

    private val homePage = "https://github.com/pponec/DirectoryBookmarks"
    private val appName = "directory-bookmarks.kts"
    private val appVersion = "1.3"
    private val storeName = ".directory-bookmarks.csv"
    private val separator = '\t'
    private val comment = '#'
    private val newLine = System.lineSeparator()
    private val header = "$comment A directory bookmarks for the '$appName' script"
    private val homeDir = System.getProperty("user.home")

    @JvmStatic
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

            "d" -> {
                if (args.size < 1) printHelpAndExit()
                delete(args[1])
            }

            "l" -> printDirectories()
            "i" -> printInstall(false)
            "i4j" -> printInstall(true) // Install for Java Runtime
            else -> printHelpAndExit()
        }
    }

    fun printHelpAndExit() {
        var bashrc = "~/.bashrc"
        println("Script '$appName' v$appVersion ($homePage)")
        println("Usage version: $appName [rwl] bookmark directory optionalComment")
        println("Integrate the script to Ubuntu: $appName i >> $bashrc && . $bashrc")
        exitProcess(1)
    }

    fun printDirectories() {
        val storeFile = getStoreFile()
        return storeFile.bufferedReader().use {
            it.lines()
                .filter { !it.startsWith(comment) }
                .sorted()
                .forEach { println("$it") }
        }
    }

    fun getDirectory(key: String, defaultDir: String): String {
        when (key) {
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

    fun delete(key: String) =
        save(key, "")

    /** Empty dir removes the bookmark */
    fun save(dir: String, key: String, vararg comments: String) {
        require(!key.contains(separator), { "the key contains a tab" })
        val extendedKey = key + separator
        val tempFile = getStoreFileTemplate()
        val storeFile = getStoreFile()
        tempFile.bufferedWriter().use { writer ->
            writer.write(header)
            writer.write(newLine)
            if (dir.isNotEmpty()) {
                writer.write("$key$separator$dir")
                if (!comments.isEmpty()) {
                    writer.write("$separator$comment")
                    comments.forEach { writer.append(" $it") }
                }
                writer.write(newLine)
            }
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

    fun printInstall(forJava: Boolean) {
        val exec = if (forJava)
            "/opt/java/default.17/bin/java -jar ~/bin/DirectoryBookmarks.jar"
        else
            "$appName"

        val msg = """
            # Shortcuts for $appName utilities:
            alias directoryBookmarksExe='$exec'
            cdf() { cd "${'$'}(directoryBookmarksExe r ${'$'}1)"; }
            sdf() { directoryBookmarksExe w "${'$'}PWD" "${'$'}@"; }
            ldf() { directoryBookmarksExe l; }
        """.trimIndent()
        println(msg)
    }
}

// Uncomment it for kscript:
MainSingleton.main(args)
