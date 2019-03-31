# Doodlebot

An Essential Scala case study using Http4s and Scala.js

To run:

1. Start the sbt in interactive mode with `sbt`
2. Run `server/run` will compile the whole project and run the server. The server also serves the frontend Scala.js files which are compiled as part of the build step
3. Navigate to `http://localhost:8080` in your web browser of choice.

When developing, you can use the command `~server/reStart` which will detect file changes, recompile them, and restart the server
