# Git Tips and Tricks

#### Create a new branch and push it to remote repository

    git branch <branch name>
    git checkout <branch name>
    git push -u origin <branchName>

#### Delete a branch

    git branch -d

#### Show current config settings

First switch to the branch and then delete

    git checkout <branch name>
    git config --list

Deleting a remote branch requires a push

    git push origin --delete

#### Set you username and email

    git config --global user.name "John Doe"
    git config --global user.email johndoe@example.com

#### Update code from remote: fetch and pull

There are two ways of getting the latest code, `fetch`&`merge` and `pull`
-`fetch`: downloads the changes from a remote repo but does *not* apply them to your code.
-`merge`: applies changes taken from `fetch` to a branch on your local repo.
-`pull`:  combines `fetch` and then a `merge` into a single command

    git pull

##### Pull a specific remote branch into a local one by passing remote branch
 
    git pull origin some/branch/dtheobald/name


#### Share code with push
The `push` command updates the remote branch on origin with the commits from your local branch.

    git push

#### commit changes
Check the status of the files

    git status

Then add the file(s) if they are new

    git add <filenames>
Then commit

    git commit -m

Alternativly specify the files

    git commit <filename> -m


#### View history

    git log

#### Pull request and Code Reviews

-login to crucible at http://crucible.worksoft.com:8060/
-click create pull request
-select your commit

TBD??
