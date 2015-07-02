package com.github.double16

import com.saucelabs.common.Utils
import com.saucelabs.saucerest.SauceREST
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import geb.gradle.cloud.BrowserSpec
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.api.tasks.testing.TestResult

import java.util.regex.Matcher
import java.util.regex.Pattern

class JavaSauceLabsPlugin implements Plugin<Project> {

  void apply(Project project) {
    project.beforeEvaluate {
      project.buildscript.dependencies {
        classpath('org.gebish:geb-gradle:0.10.0') {
          exclude module: 'groovy-all'
        }
        classpath 'com.saucelabs:saucerest:1.0.25'
        classpath 'com.saucelabs:sauce_java_common:2.1.10'
      }
    }

    project.apply(plugin: 'java')
    project.apply(plugin: 'geb-saucelabs')

    project.sourceSets.create('functionalTest')

    project.repositories {
      maven {
        url "https://repository-saucelabs.forge.cloudbees.com/release"
      }
    }
    project.dependencies {
      sauceConnect "com.saucelabs:ci-sauce:1.84"

      functionalTestCompile 'junit:junit:4.12'
      functionalTestCompile 'org.seleniumhq.selenium:selenium-java:2.46.0'
      functionalTestCompile 'com.codeborne:phantomjsdriver:1.2.1'
      functionalTestCompile 'com.saucelabs:saucerest:1.0.25'
      functionalTestCompile 'com.saucelabs:sauce_java_common:2.1.10'
    }

    project.extensions.browsers = project.container(BrowserSpec) { String name ->
      new BrowserSpec('saucelabs', name)
    }

    def functionalTests = project.tasks.create("functionalTests") {
      group "Functional Test"
    }

    project.tasks.create(name: "phantomJsTest", type: Test) {
      def reportDir = project.file("${project.buildDir}/test-results/phantomjs")
      group functionalTests.group()
      testClassesDir = project.sourceSets.functionalTest.output.classesDir
      classpath = project.sourceSets.functionalTest.runtimeClasspath
      reports.junitXml.destination = reportDir
      reports.html.destination = reportDir
      binResultsDir = reportDir
      systemProperty 'functionalTests.resultsDir', reportDir.getCanonicalPath()
      if (project.plugins.findPlugin('org.akhikhl.gretty')) {
        dependsOn 'appBeforeIntegrationTest'
        finalizedBy 'appAfterIntegrationTest'
      }
    }
    functionalTests.dependsOn "phantomJsTest"

    project.extensions.browsers.all { BrowserSpec browserSpec ->
      def reportDir = project.file("${project.buildDir}/test-results/${browserSpec.displayName}")
      Test task = project.tasks.create(name: "${browserSpec.displayName}Test", type: Test) {
        def account = project.extensions.sauceLabs.account
        def sauceListener = new SauceListener(account.username, account.accessKey)
        addTestListener(sauceListener)
        addTestOutputListener(sauceListener)

        group functionalTests.group()
        testClassesDir = project.sourceSets.functionalTest.output.classesDir
        classpath = project.sourceSets.functionalTest.runtimeClasspath
        reports.junitXml.destination = reportDir
        reports.html.destination = reportDir
        binResultsDir = reportDir
        systemProperty 'functionalTests.resultsDir', reportDir.getCanonicalPath()
        systemProperty 'saucelabs.job-name', project.name
        systemProperty 'saucelabs.build', project.version
        if (project.plugins.findPlugin('org.akhikhl.gretty')) {
          dependsOn 'appBeforeIntegrationTest'
          finalizedBy 'appAfterIntegrationTest'
        }
      }
      browserSpec.testTask = task
      browserSpec.configureTestTask()
      String browserStr = browserSpec.testTask.getSystemProperties().get('geb.saucelabs.browser')
      task.systemProperty 'saucelabs.browser', browserStr
      if (!browserStr.contains("url=")) {
        task.dependsOn 'openSauceTunnelInBackground'
        task.finalizedBy 'closeSauceTunnel'
      }

      functionalTests.dependsOn "${browserSpec.displayName}Test"
    }
  }
}

class SauceListener implements TestListener, TestOutputListener {
  private static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=(.+)");

  private SauceREST sauceREST;
  private String sessionId;

  SauceListener(String username, String accessKey) {
    this.sauceREST = new SauceREST(username, accessKey);
  }

  void beforeTest(TestDescriptor testDescriptor) { }
  void beforeSuite(TestDescriptor suite) { }
  void afterTest(TestDescriptor testDescriptor, TestResult result) { }

  void afterSuite(TestDescriptor suite, TestResult result) {
    if (sessionId) {
      if (result.getResultType() == TestResult.ResultType.FAILURE) {
        markJobAsFailed(sessionId);
      }
      if (result.getResultType() == TestResult.ResultType.SUCCESS) {
        markJobAsPassed(sessionId);
      }
    }
  }

  void onOutput(TestDescriptor testDescriptor, TestOutputEvent outputEvent) {
    Matcher matcher = SESSION_ID_PATTERN.matcher(outputEvent.getMessage());
    if (matcher.find()) {
      sessionId = matcher.group(1);
    }
  }

  /**
   * Marks a Sauce job as failed.
   * @param sessionId the Sauce job id
   */
  private void markJobAsFailed(String sessionId) {
    try {
      if (this.sauceREST && sessionId) {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", false);
        Utils.addBuildNumberToUpdate(updates);
        sauceREST.updateJobInfo(sessionId, updates);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Marks a Sauce job as passed.
   * @param sessionId the Sauce job id
   */
  private void markJobAsPassed(String sessionId) {
    try {
      if (this.sauceREST && sessionId) {
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("passed", true);
        Utils.addBuildNumberToUpdate(updates);
        sauceREST.updateJobInfo(sessionId, updates);
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException(ioe);
    }
  }
}
