# Template Based Question Answering
## Installation
- Add the AKSW repository to your maven settings.xml file:
```xml
<settings>
  <profiles>
    <profile>
      <id>Repository Proxy</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <repositories>
        <repository>
          <id>aksw</id>
          <url>http://maven.aksw.org/repository/internal/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
      </pluginRepositories>
    </profile>
  </profiles>
</settings>
```
