FROM eclipse-temurin:17
VOLUME /tmp
COPY build/libs/web-1.0.1.war app.war
ENTRYPOINT ["java","-jar","/app.war"]
