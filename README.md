# bi-jooq-codegen
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FBreeding-Insight%2Fbi-jooq-codegen.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2FBreeding-Insight%2Fbi-jooq-codegen?ref=badge_shield)


This is an extension of JOOQ's codegen library, and adds the following when generating:

- DAOs have `@Inject` at the constructor level (added to the constructor like: `public TableDao(Configuration configuration)`)
- POJOs have `@SuperBuilder` added at the class level

To include this dependency in your project, add the following dependency to the `jooq-codegen-maven` plugin:

```
<dependency>
  <groupId>org.breedinginsight</groupId>
  <artifactId>bi-jooq-codegen</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

And in your JOOQ generation configuration, add:

`<name>org.breedinginsight.generation.JooqDaoGenerator</name>`

under the `generator` node. 

### settings.xml
Currently, this package ***IS NOT*** in Maven Central, but hosted in GitHub Packages, you will need to modify your `settings.xml` file to add the following repository to the `<pluginRepositories>` tag:

```
<pluginRepository>
    <id>github</id>
    <name>GitHub Breeding Insight Apache Maven Packages</name>
    <url>https://maven.pkg.github.com/Breeding-Insight/bi-jooq-codegen</url>
</pluginRepository>
```

You will also need to add a `server` configuration item:

```
<server>
    <id>github</id>
    <username>${env.GITHUB_ACTOR}</username>
    <password>${env.GITHUB_TOKEN}</password>
</server>
```

`GITHUB_ACTOR` - your GitHub username  
`GITHUB_TOKEN` - You can generate a token for authenticating to GitHub's package manager by following these instructions: https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line.  
*Note: your token needs to at least have `read:package` rights.

An example `settings.xml` file:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>

    <profiles>
        <profile>
            <id>github</id>
            <pluginRepositories>
                <pluginRepository>
                    <id>central</id>
                    <url>https://repo1.maven.org/maven2</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </pluginRepository>
                <pluginRepository>
                    <id>github</id>
                    <name>GitHub Breeding Insight Apache Maven Packages</name>
                    <url>https://maven.pkg.github.com/Breeding-Insight/bi-jooq-codegen</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <servers>
        <server>
            <id>github</id>
            <username>${env.GITHUB_ACTOR}</username>
            <password>${env.GITHUB_TOKEN}</password>
        </server>
    </servers>
</settings>
```

*If you don't want to modify your global `settings.xml`, you can create one to live in your code, then when running maven commands, include the `--settings` flag, ex: `mvn clean install --settings settings.xml`


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2FBreeding-Insight%2Fbi-jooq-codegen.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2FBreeding-Insight%2Fbi-jooq-codegen?ref=badge_large)