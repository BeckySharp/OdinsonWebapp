Clean install of OdinsonWebapp on Mac OS

## Prereqs
Install these if you have not already -- they provide much more functionality than just for Scala/OdinsonWebapp

(1) Ensure [Mac command-line-tools](http://osxdaily.com/2014/02/12/install-command-line-tools-mac-os-x/) is installed:

    xcode-select --install

(2) Install [Homebrew](https://brew.sh/).
In particular, execute the following from the command-line:

    /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"


## Install Java 8
(1) Download [Java 8 JDK](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)

(2) After installation, from the command line, verify the java location and version, which should be 1.8 .  E.g.,:
        
        claytonm@shadow ~ $ which java
        /usr/bin/java
        claytonm@shadow ~ $ java -version
        java version "1.8.0_172"
        Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
        Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)


### Install Scala Build Tool (sbt)

Using Homebrew, execute the following from the command-line:
        
        brew install scala
        brew install sbt

### Install Scala/OdinsonWebapp

(1) clone the OdinsonWebapp repo:

        git clone git@github.com:BeckySharp/OdinsonWebapp.git

(2) cd into the root of the repo and run:

        cd OdinsonWebapp
        sbt webapp/run

That will start the process of downloading all sbt/scala dependencies
NOTE: This will take a while and you want a good internet connection
Background: sbt and Scala naturally sandbox themselves so that *all* associated file, include the specific version of sbt and scala that are required by your project, get placed in:
   
   > ~/.sbt    # stores specific version of sbt
   
   > ~/.ivy2   # stores specific version of scala and project dependencies

When you run sbt for the first time on your project, it uses the project definition to determine the specific version of sbt and scala needed and downloads everything; thereafter, when you run again, it just uses the cached versions.

After successfully downloading everything needed, the call to webapp/run then launches the webapp.  The webapp will stay running in the console.

You can now point your browser to `localhost:9000` to connect to the webapp html interface.  

To STOP the webapp, hit `control-c` in the terminal.