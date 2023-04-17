# DirectoryBookmarks.kts

Simple [Kotlin script](https://github.com/kscripting/kscript) for manage (writing and reading) directories (of the filesystem) by text bookmarks.

## Usage:

Brief usage:

* `directory-bookmarks.kts h`

Read the directory by a bookmark text:

* `directory-bookmarks.kts r [bookmark]`

Write a new bookmark:

* `directory-bookmarks.kts w [bookmark] [directory]`

List the all saved bookmarks:

* `directory-bookmarks.kts w [bookmark] [directory]`

Generate bash code for an integration with Linux Bash:

* `directory-bookmarks.kts i`

For integration with Linux Ubuntu, it is advisable to add some functions (from the the last command) to the end of the `.bashrc` file. 
After reloading the `.bashrc` file, the following shortcuts can be used:

* `sdf [bookmark]` # Save the current directory to the required bookmark
* `cdf [bookmark]` # Change a current directory by a bookmark
* `ldf` # list all saved bookmarks and directories


## Installation for Linux Ubuntu:

Backup the file `.bashrc` before.

* Install kscript according the instruction: https://github.com/kscripting/kscript
* `cd ~/bin && wget https://raw.githubusercontent.com/pponec/DirectoryBookmarks/main/directory-bookmarks.kts`
* `chmod 755 directory-bookmarks.kts`
* `directory-bookmarks.kts i >> ~/.bashrc && . ~/.bashrc`

