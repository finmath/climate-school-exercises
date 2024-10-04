# Climate School Exercises


## Work in Progress

**Note: This repository is still under construction. It will be populated until the start of the lecture.**

**(Check back and pull updates if you are curious)**

------


## Importing in Eclipse from GitHub

Import this git repository into Eclipse and start working.

- Got to this repository on GitHub
- Click on “Clone or download” and copy the URL to your clipboard.
- Go to Eclipse and select File → Import → Git → Projects from Git **(with smart import)**.
- Select “Clone URI” and paste the GitHub URL from step 2.
- Select "main", then Next → Next → Finish.

Note: If you choose "Projects from Git" without the option "(with smart import)" you may expirience that
the project is not imported into Eclipse, but it was successfully checked out via git, i.e. you
find the project files in you local git folder. In that case, you can import the project "as maven project"
(see below).

### Importing in Eclipse (as Maven Project) (Alternatively)

If you checked out the git repository manually (`git clone`), then import
the local git folder as Maven Project;

- File → Import → Maven → Existing Maven Projects
- Select the project folder in you *local* git folder.

## Testing your Setup

To test your setup, run the Java Class `Test.java` in the package `net.finmath.climateschool.begin`. To do so: In the Eclipse Project Explorer:

- Expand `src/main/java'
- Expand `net.finmath.climateschool.begin`
- Right-click on the class `Test.java`,
- then select “Run As → Java Application".
  
## Update the Project (later)

To get an update of this project at a later time

- Right click on the project,
- then select “Team → Pull".
 
 This will *pull* updates committed to the project.
 
Note: If you modified files in the project, you may see "merge conflicts". At the current stage it is recommended that you do not modify existing files. You may add new ones.