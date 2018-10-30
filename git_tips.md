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

Then add the file(s) to the "stage" to be committed

`git add <filenames>`  or `git add -i` for interactive mode, see the git add --help for usage of interactive staging
    
Then commit:

    git commit -m "INT-123: commit message text"
Omit the -m flag if you wish to use a text editor to enter the commit message

Alternativly specify the files

    git commit <filename> -m "INT-123: commit message text"


#### View history

    git log

#### Pull request and Code Reviews
- push your code changes to your named branch
- login to crucible at http://crucible.worksoft.com:8060/
- click "Create review"
- click Edit Details button
  - enter a title
  - add reviewers
  - add a description of changes
  - add any other information you wish/need
- click "Add Content" button
  - click Choose branches
  - select your branch
  - changed "Branched from" to the destination branch

what else TBD??

#### Set diff and merge tools to an external tool

- **Beyond Compare**
  - merge tool
    - `git config --global merge.tool bc`
    - `git config --global mergetool.bc.path "c:/Program Files (x86)/Beyond Compare 4/bcomp.exe"`
  - diff tool
     - `git config --global diff.tool bc`
     - `git config --global difftool.bc.path "c:/Program Files (x86)/Beyond Compare 4/bcomp.exe"`
- **Ediff**
  - `git config --global diff.tool ediff`

**Usage:**
Use the `git difftool` command
For example: `git difftool INT-609 origin/INT-609 --dir-diff`


#### Archiving Old Branches 

First, ensure any commits that are to be kept have been pushed to the parent branch.  Then push any outstanding local commits to the origin. 

1. Tag the branch using the `archive/` folder
    `git tag archive/<branchname> <branchname>`
2. Delete branch from local Git working copy
    `git branch -d <branchname>`
3. Delete the branch from the remote Git origin
    `git push origin :branchname`
4. Push the new tags to origin
    `git push --tags`
    
    
#### Restore from an archived branch tag

    `git checkout -b branchname archive/branchname`
    
The -b flag tells Git to create a new branch and check it out in the same step. 
The first branch name is the name of the new branch, and the second is the name of the tag to use as the source for the new branch.