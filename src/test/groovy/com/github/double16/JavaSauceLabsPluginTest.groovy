package com.github.double16

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.assertTrue

class JavaSauceLabsPluginTest {
  @Test
  void pluginCreatesBrowserConfiguration() {

  }

  @Test
  void pluginCreatePhantomJSTask() {
    Project project = ProjectBuilder.builder().build()
    project.apply plugin: 'com.github.double16.java-saucelabs'

    assertTrue(String.valueOf(project.tasks.phantomJsTest), project.tasks.phantomJsTest instanceof org.gradle.api.tasks.testing.Test)
  }
}
