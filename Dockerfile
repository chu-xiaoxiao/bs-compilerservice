FROM cent/jdk8
VOLUME /tmp
ADD /target/compile-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]