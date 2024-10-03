# Climate School Exercises



## Importing in Eclipse from GitHub

Import this git repository into Eclipse and start working.

- Got to this repository on GitHub
- Click on “Clone or download” and copy the URL to your clipboard.
- Go to Eclipse and select File -> Import -> Git -> Projects from Git **(with smart import)**.
- Select “Clone URI” and paste the GitHub URL from step 2.
- Select "main", then Next -> Next -> Finish.

Note: If you choose "Projects from Git" without the option "(with smart import)" you may expirience that
the project is not imported into Eclipse, but it was successfully checked out via git, i.e. you
find the project files in you local git folder. In that case, you can import the project "as maven project"
(see below).

### Importing in Eclipse (as Maven Project)

If you checked out the git repository manually (`git clone`), then import
the local git folder as Maven Project;

- File -> Import -> Maven -> Existing Maven Projects
- Select the project folder in you *local* git folder.
