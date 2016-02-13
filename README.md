

Configuration
-------------

All config is set via environment variables.

    # The address to start the web server on
    appserver.port=8080
    
    # A directory where all the repos, log files, temp data etc will be stored
    appserver.data.dir=/opt/app-runner-data
    
### Specifying location of Git Repos

You can have a file with a git repo URL on each line:

    appserver.git.repo.list.path=/path/to/repos.txt
