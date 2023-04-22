# DirectoryBookmarks.kts

Simple [Kotlin script](https://github.com/kscripting/kscript) for manage (writing and reading) directories.
The script can be used to save the current directory along with a unique text identifier, which can then be used to quickly return to the saved directory. 
The script supports adding bookmark, listing and deleting. 

After an integration to a Bash be:

* `sdf [bookmark] [optional comment]` # Save the current directory to the required bookmark
* `cdf [bookmark]` # Change a current directory by a bookmark
* `ldf` # list all saved bookmarks and directories


## Connon Usage:

Brief usage (help):

* `directory-bookmarks.kts h`

Read the directory by a bookmark text:

* `directory-bookmarks.kts r [bookmark]`

Write a new bookmark:

* `directory-bookmarks.kts w [bookmark] [directory] [optional comment]`

List the all saved bookmarks:

* `directory-bookmarks.kts l`

Delete a bookmark:

* `directory-bookmarks.kts d [bookmark]`

Generate bash code for an integration with Linux Bash:

* `directory-bookmarks.kts i`

For integration with Linux Ubuntu, it is advisable to add some functions (from the the last command) to the end of the `.bashrc` file. 
After reloading the `.bashrc` file, the command shortcuts presented before (`sdf`, `cdf`, `ldf`).


## Installation for Linux Ubuntu:

Backup the file `.bashrc` before.

* Install kscript according the instructions: https://github.com/kscripting/kscript
* Switch to the executable scripts directory: `cd ~/bin` 
* `wget https://raw.githubusercontent.com/pponec/DirectoryBookmarks/main/directory-bookmarks.kts`
* `chmod 755 directory-bookmarks.kts`
* `directory-bookmarks.kts i >> ~/.bashrc && . ~/.bashrc`

## Similar projects:

* https://github.com/C-Hess/cd-bookmark
* https://github.com/wting/autojump/wiki


