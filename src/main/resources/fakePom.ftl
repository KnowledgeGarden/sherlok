<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Automatically generated by Sherlok at ${now} -->

  <groupId>org.sherlok</groupId>
  <artifactId>${pipelineName}</artifactId>
  <version>${pipelineVersion}</version>
  
  <dependencies>
    <!-- latest (unreleased), for Json writer -->
    <dependency>
      <groupId>org.apache.uima</groupId>
      <artifactId>uimaj-core</artifactId>
      <version>2.6.1-json</version>
      <scope>provided</scope>
    </dependency>
  <#list deps as dep>
    <dependency>
      <groupId>${dep.groupId}</groupId>
      <artifactId>${dep.artifactId}</artifactId>
      <version>${dep.version}</version>
    </dependency>
  </#list>
  </dependencies>

  <repositories>
  <#list repos as repo>
    <repository>
      <id>${repo.id}</id>
      <url>${repo.url}</url>
    </repository>
  </#list>
  </repositories>

</project>