# OdinsonWebapp


This is a webapp designed to facilitate Odinson rule development.
Odinson is a highly optimized information extraction framework that 
supports *real-time* queries against an indexed corpus.
For more information, please see [Odinson's documentaion](https://github.com/lum-ai/odinson).


To use this webapp, I assume you already have an [Odinson index made](https://github.com/lum-ai/odinson/tree/master/extra).

 - Point the webapp to the index (not the parent dir).  In `src/main/resources/application.conf` 
 set: 
    
        odinson.indexDir = "path/to/index"

- on the command line, run: 

        sbt webapp/run
        
This will launch the webapp.  Then, in a browser, go to `localhost:9000`

You should see something like this:

![image](docs/images/main_screen.png)

To try out a rule, hit the `SUBMIT` button.
Results are displayed in a table, initially sorted by frequency.

![image](docs/images/results.png)

To see the supporting evidence, click the `+` button for the row.

![image](docs/images/evidence.png)

To export the contents of the table, select `export matches` and slick `submit`.
The matches will be saved as a json lines file (i.e., one json object per match, one per line).
The file will be saved in the current directory, in a file named by the rule name concatenated
with the local datetime (e.g., `example-rule_2020-04-08_10:47:36.jsonl`).

![image](docs/images/export.png)

## Docker

You can build this into a container with:

```
docker build -t odinsonwebapp .
```

This generates a container named `odinsonwebapp`, which can be run using:

```
docker run -it --rm -p 9000:9000 -d odinsonwebapp
```

Any feedback is much appreciated!
