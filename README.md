# Simple compiler for the C-- programming language
- To compile the program enter `mvn package`. This will create multiple jars in `.target/`. Usually the one containing `shaded` is a fat jar containing all dependencies. This will also run tests.
- To run only the unit tests enter `mvn test`.
- To generate JavaDoc enter `mvn javadoc:javadoc` the generated doc will be found in `./target/site/index.html`.

## Prerequesites for building the jar

Jasmin has to be installed in the local Maven repo. Under following attributes (given the jasmin jar is just called `jasmin.jar`):  
- `groupId = org.jasmin`
- `artifactId = jasmin`
- `version = 2.4`

The following command should install it according to mentioned criteria: `mvn install:install-file -Dfile=Path/to/jasmin.jar -DgroupId=org.jasmin -DartifactId=jasmin -Dversion=2.4 -Dpackaging=jar`
