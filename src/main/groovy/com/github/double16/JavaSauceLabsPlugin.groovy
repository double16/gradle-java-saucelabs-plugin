package com.github.double16

import com.saucelabs.gradle.SauceListener
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import geb.gradle.cloud.BrowserSpec

class JavaSauceLabsPlugin implements Plugin<Project> {

  void apply(Project project) {
    project.beforeEvaluate {
      project.buildscript.dependencies {
        classpath 'com.saucelabs.gradle:sauce-gradle-plugin:0.0.1'
        classpath 'org.gebish:geb-gradle:0.10.0'
      }
    }

    project.apply(plugin: 'geb-saucelabs')

    project.sourceSets.create('functionalTest')

    project.repositories {
      maven {
        url "https://repository-saucelabs.forge.cloudbees.com/release"
      }
    }
    project.dependencies {
      sauceConnect "com.saucelabs:ci-sauce:1.84"

      functionalTestCompile 'junit:junit:4.11'
      functionalTestCompile 'org.seleniumhq.selenium:selenium-java:2.42.2'
      functionalTestCompile 'com.github.detro:phantomjsdriver:1.2.0'
      functionalTestCompile 'com.saucelabs:sauce_java_common:2.1.10'
    }

    project.extensions.browsers = project.container(BrowserSpec) { String name ->
      new BrowserSpec('saucelabs', name)
    }

    def functionalTests = project.tasks.create("functionalTests") {
      group "Functional Test"
    }

    project.tasks.create(name: 'sauceListener') {
      doFirst {
        def account = project.extensions.sauceLabs.account
        project.gradle.addListener(new SauceListener(account.username, account.accessKey))
      }
    }

    project.tasks.create(name: "phantomJsTest", type: Test) {
      group functionalTests.group()
      testClassesDir = project.sourceSets.functionalTest.output.classesDir
      classpath = project.sourceSets.functionalTest.runtimeClasspath
      def reportDir = project.file("${project.buildDir}/test-results/phantomjs")
      reports.junitXml.destination = reportDir
      reports.html.destination = reportDir
      binResultsDir = reportDir
      systemProperty 'proj.test.resultsDir', reportDir.getCanonicalPath()
      dependsOn 'sauceListener'
      if (project.plugins.findPlugin('org.akhikhl.gretty')) {
        dependsOn 'appBeforeIntegrationTest'
        finalizedBy 'appAfterIntegrationTest'
      }
    }
    functionalTests.dependsOn "phantomJsTest"

    project.extensions.browsers.all { BrowserSpec browserSpec ->
      def reportDir = project.file("${project.buildDir}/test-results/${browserSpec.displayName}")
      def task = project.tasks.create(name: "${browserSpec.displayName}Test", type: Test) {
        group functionalTests.group()
        testClassesDir = project.sourceSets.functionalTest.output.classesDir
        classpath = project.sourceSets.functionalTest.runtimeClasspath
        reports.junitXml.destination = reportDir
        reports.html.destination = reportDir
        binResultsDir = reportDir
        systemProperty 'proj.test.resultsDir', reportDir.getCanonicalPath()
        systemProperty 'saucelabs.job-name', project.name
        systemProperty 'saucelabs.build', project.version
        dependsOn 'sauceListener', 'openSauceTunnelInBackground'
        finalizedBy 'closeSauceTunnel'
        if (project.plugins.findPlugin('org.akhikhl.gretty')) {
          dependsOn 'appBeforeIntegrationTest'
          finalizedBy 'appAfterIntegrationTest'
        }
      }
      browserSpec.testTask = task
      browserSpec.configureTestTask()
      task.systemProperty 'saucelabs.browser', browserSpec.testTask.getSystemProperties().get('geb.saucelabs.browser')

      functionalTests.dependsOn "${browserSpec.displayName}Test"
    }
  }

}
