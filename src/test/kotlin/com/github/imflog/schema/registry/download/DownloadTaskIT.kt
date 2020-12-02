package com.github.imflog.schema.registry.download

import com.github.imflog.schema.registry.TestContainersUtils
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.json.JsonSchema
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import org.assertj.core.api.Assertions
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream

class DownloadTaskIT : TestContainersUtils() {

    private lateinit var folderRule: TemporaryFolder
    private lateinit var buildFile: File

    @BeforeEach
    fun init() {
        folderRule = TemporaryFolder()
        folderRule.create()
    }

    @AfterEach
    fun tearDown() {
        folderRule.delete()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SchemaArgumentProvider::class)
    fun `Should download schemas`(type: String, oldSchema: ParsedSchema, newSchema: ParsedSchema) {
        // Given
        val subjectName = "parameterized-$type"

        client.register(subjectName, oldSchema)
        client.register(subjectName, newSchema)

        buildFile = folderRule.newFile("build.gradle")
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'com.github.imflog.kafka-schema-registry-gradle-plugin'
            }

            schemaRegistry {
                url = '$schemaRegistryEndpoint'
                download {
                    subject('$subjectName', '${folderRule.root.absolutePath}/src/main/$type/test')
                    subject('$subjectName', 'src/main/$type/test_v1', 1)
                    subject('$subjectName', 'src/main/$type/test_v2', 2)
                }
            }
        """
        )

        // When
        val result: BuildResult? = GradleRunner.create()
            .withGradleVersion("6.2.2")
            .withProjectDir(folderRule.root)
            .withArguments(DownloadTask.TASK_NAME)
            .withPluginClasspath()
            .withDebug(true)
            .build()

        // Then
        val schemaFile = "$subjectName.${oldSchema.extension()}"
        Assertions.assertThat(File(folderRule.root, "src/main/$type/test")).exists()
        Assertions.assertThat(File(folderRule.root, "src/main/$type/test/$schemaFile")).exists()
        Assertions.assertThat(result?.task(":downloadSchemasTask")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        Assertions.assertThat(File(folderRule.root, "src/main/$type/test_v1")).exists()
        val resultFile1 = File(folderRule.root, "src/main/$type/test_v1/$schemaFile")
        Assertions.assertThat(resultFile1).exists()
        Assertions.assertThat(resultFile1.readText()).doesNotContain("description")

        Assertions.assertThat(File(folderRule.root, "src/main/$type/test_v2")).exists()
        val resultFile2 = File(folderRule.root, "src/main/$type/test_v2/$schemaFile")
        Assertions.assertThat(resultFile2).exists()
        Assertions.assertThat(resultFile2.readText()).contains("description")
    }

    @Test
    fun `DownloadSchemaTask should fail download when schema does not exist`() {
        buildFile = folderRule.newFile("build.gradle")
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'com.github.imflog.kafka-schema-registry-gradle-plugin'
            }

            schemaRegistry {
                url = '$schemaRegistryEndpoint'
                download {
                    subject('UNKNOWN', 'src/main/avro/test')
                }
            }
        """
        )

        val result: BuildResult? = GradleRunner.create()
            .withGradleVersion("6.2.2")
            .withProjectDir(folderRule.root)
            .withArguments(DownloadTask.TASK_NAME)
            .withPluginClasspath()
            .withDebug(true)
            .buildAndFail()
        Assertions.assertThat(result?.task(":downloadSchemasTask")?.outcome).isEqualTo(TaskOutcome.FAILED)
    }

    private class SchemaArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext): Stream<out Arguments> =
            Stream.of(
                Arguments.of(
                    AvroSchema.TYPE,
                    AvroSchema(
                        """{
                        "type": "record",
                        "name": "User",
                        "fields": [
                            { "name": "name", "type": "string" }
                        ]
                    }"""
                    ),
                    AvroSchema(
                        """{
                        "type": "record",
                        "name": "User",
                        "fields": [
                            { "name": "name", "type": "string" }, 
                            { "name": "description", "type": ["null", "string"], "default": null }
                        ]
                    }"""
                    )
                ),
                Arguments.of(
                    JsonSchema.TYPE,
                    JsonSchema(
                        """{
                        "properties": {
                            "name": {"type": "string"}
                        },
                        "additionalProperties": false
                    }"""
                    ),
                    JsonSchema(
                        """{
                        "properties": {
                            "name": {"type": "string"},
                            "description": {"type": "string"}
                        },
                        "additionalProperties": false
                    }"""
                    )
                ),

                Arguments.of(
                    ProtobufSchema.TYPE,
                    ProtobufSchema(
                        """
                        syntax = "proto3";
                        option java_outer_classname = "User";

                        message TestMessage {
                            string name = 1;
                        }
                    """
                    ),
                    ProtobufSchema(
                        """
                        syntax = "proto3";
                        option java_outer_classname = "User";
                        
                        message TestMessage {
                            string name = 1;
                            string description = 2;
                        }
                    """
                    )
                )
            )
    }
}
