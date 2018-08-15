FROM maven
WORKDIR /
# Config and data
COPY . .
# Execute main UI
EXPOSE 8080

CMD ["mvn", "clean", "spring-boot:run"]
