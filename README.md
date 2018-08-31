[![CircleCI](https://circleci.com/gh/ImFlog/schema-registry-plugin/tree/master.svg?style=svg)](https://circleci.com/gh/ImFlog/schema-registry-plugin/tree/master)

# Schema-registry-plugin
The aim of this plugin is to adapt the [Confluent schema registry maven plugin](https://docs.confluent.io/current/schema-registry/docs/maven-plugin.html) for Gradle builds.

See [gradle plugins portal](https://plugins.gradle.org/plugin/com.github.imflog.kafka-schema-registry-gradle-plugin)
for instructions about how to add the plugin to your build configuration.

When you do so, three tasks are added under registry group:
* downloadSchemasTask
* testSchemasCompatibilityTask
* registerSchemasTask
What these tasks do and how to configure them is described in the following sections.
## Download schemas
Like the name of the task imply, this task is responsible of retrieving schemas from a schema registry.

A DSL is available to configure the task:
```groovy
schemaRegistry {
    url = 'http://localhost:8081/'
    download {
        subject('topic1-key', 'src/main/avro')
        subject('topic1-value', 'src/main/avro/values')
    }
}
```
You have to put the url where the script can reach the Schema Registry.

You need to specify the pairs (subjectName, outputDir) for all the
schemas you want to download. 

## Test schemas compatibility
This task test compatibility between local schemas and schemas stored in the Schema Registry.

A DSL is available to specify what to test:
```groovy
schemaRegistry {
    url = 'http://localhost:8081'
    compatibility {
        subject('mySubject', 'file/path')
        subject('otherSubject', 'other/path')
    }
}
```
You have to put the url where the script can reach the Schema Registry.

You have to list all the (subject, avsc file path) pairs that you want to test. 

## Register schemas
Once again the name speaks for itself.
This task register schemas from a local path to a Schema Registry.

A DSL is available to specify what to register:
```groovy
schemaRegistry {
    url = 'http://localhost:8081'
    register {
        subject('mySubject', 'file/path')
        subject('otherSubject', 'other/path')
        subject('subjectWithDependencies', 'dependent/path', ['firstDependency/path', 'secondDependency/path'])
    }
}
```
You have to put the url where the script can reach the Schema Registry.

You have to list all the (subject, avsc file path) pairs that you want to send.

If you have dependencies with other schemas required before the register phase,
you can add a third parameter with the needed paths.

The order of the file paths in the list is significant.
Any Avro Schema that contains another type must be after the other type in the list.
So if you have a user-profile.avsc that references a type in address.avsc,
you should declare the list as follows:
```groovy
['address.avsc', 'user-profile.avsc']
```