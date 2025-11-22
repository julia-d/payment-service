Java style: google-java-format
Tech Stack: Java 21, Spring Boot, Maven, Spring Data JPA, SQLLite, H2, FlyWay, gRPC
Use Lombok to reduce boilerplate code.
Use java record for data carrier classes.
Use SQL Lite for development and H2 for testing.
Do not add comments to java and SQL files unless I have asked for them.
Do not use var. Use explicit types.
When creating entities and DTOs with a builder pattern, structure all the methods one under another, not in a line.

Tests: aim to 80% code coverage. Test success and edge cases. Use mocks for unit tests. Make parameterized tests.   

