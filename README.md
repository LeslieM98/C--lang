# Simple compiler for the C-- programming language
- To compile the program enter `mvn package`. This will create multiple jars usually the one containing `shaded` is a fat jar containing all dependencies. This will also run tests.
- To run only the unit tests enter `mvn test`
- To generate JavaDoc enter `mvn javadoc:javadoc` the generated doc will be found in `./target/site/index.html`