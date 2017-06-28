# Template Based Question Answering
## Installation
- Add the AKSW repository to your maven settings.xml file:
```xml
<settings>
  <!-- omitted xml -->
  <profiles>
    <profile>
      <id>Repository Proxy</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <!-- ******************************************************* -->
      <!-- repositories for jar artifacts -->
      <!-- ******************************************************* -->
      <repositories>
        <repository>
          <id>aksw</id>
          <url>http://maven.aksw.org/repository/internal/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
         <repository>
          <id>aksw</id>
          <url>http://maven.aksw.org/repository/snapshots/</url>
          <releases>
            <enabled>true</enabled>
          </releases>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
      <!-- ******************************************************* -->
      <!-- repositories for maven plugins -->
      <!-- ******************************************************* -->
      <pluginRepositories>
        <!-- omitted xml -->   
      </pluginRepositories>
    </profile>
    <!-- omitted xml -->
  </profiles>
  <!-- omitted xml -->
</settings>
```
