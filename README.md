# DirectoryBookmarks.kts

Simple Kotlin script for writing and reading directories (from the filesystem) using backups.

## Usage:

Read the directory by a bookmark text:

* directory-bookmarks.kts r [bookmark]

Write a new bookmark:

* directory-bookmarks.kts w [bookmark] [directory]

List off the saved bookmarks:

* directory-bookmarks.kts w [bookmark] [directory]

Breaf usage:

* directory-bookmarks.kts h

Generate bash code for an integration with Linux Bash:

* directory-bookmarks.kts i

----Add the generated function to create to the `.bashrc` file and reloaded it.

For integration with Linux, it is advisable to add the code generated (by the the last command) to the end of the `.bashrc` file. After reloading the .bashrc file, the following shortcuts can be used:

* sdf [bookmark] # Save the current directory to bookmark
* cdf [bookmark] # Change a current directory by a bookmark
* ldf # list all saved bookmarks


## Installation for Linux Ubuntu:

Backup the file `.bashrc` before.

* Install kscript according the instruction: https://github.com/kscripting/kscript
* cd ~/bin && wget https://raw.githubusercontent.com/pponec/DirectoryBookmarks/main/directory-bookmarks.kts 
* chmod 755 directory-bookmarks.kts
* directory-bookmarks.kts i >> ~/.bashrc && . ~/.bashrc

