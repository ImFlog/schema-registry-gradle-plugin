package com.github.imflog.schema.registry

import com.github.imflog.schema.registry.tasks.config.ConfigTask
import org.assertj.core.api.Assertions
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class SslIT {

    private val folderRule: TemporaryFolder = TemporaryFolder()
    private lateinit var buildFile: File

    @BeforeEach
    fun init() {
        folderRule.create()
    }

    @AfterEach
    fun tearDown() {
        folderRule.delete()
    }

    @Test
    fun `Should fail with incorrect ssl property`() {
        buildFile = folderRule.newFile("build.gradle")
        buildFile.writeText(
            """
            plugins {
                id 'java'
                id 'com.github.imflog.kafka-schema-registry-gradle-plugin'
            }

            schemaRegistry {
                url = 'https://localhost:8181/'
                ssl {
                    configs = ["foo": "bar"]
                }
                config {
                    subject('testSubject1', 'FULL_TRANSITIVE')
                }
            }
            """
        )

        val result: BuildResult? = GradleRunner.create()
            .withGradleVersion("6.7.1")
            .withProjectDir(folderRule.root)
            .withArguments(ConfigTask.TASK_NAME)
            .withPluginClasspath()
            .withDebug(true)
            .buildAndFail()
        Assertions.assertThat(result?.task(":configSubjectsTask")?.outcome).isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `Should register schema using SSL`() {
        TODO("Start a Schema Registry with SSL programmatically")
    }
}
