gradle-java-saucelabs-plugin
============================

Gradle Plugin to help integrate Sauce Labs functional testing into a Java/JUnit stack. The Gradle build will create
the tasks necessary to run the tests, including starting a Sauce Connect tunnel. The JUnit tests require some integration
which is provided by a base test class.

# build.gradle
Work the following template into your build.gradle file:

```groovy
buildscript {
    repositories {
        maven {
            url "https://repository-saucelabs.forge.cloudbees.com/release"
        }
    }
    dependencies {
        classpath 'org.gebish:geb-gradle:0.10.0' // for the sauceLabs configuration
        classpath 'com.saucelabs:saucerest:1.0.22'
        classpath 'com.saucelabs:sauce_java_common:2.1.10'
        classpath 'com.github.double16:gradle-java-saucelabs-plugin:0.1-SNAPSHOT' // this plugin
        classpath "org.akhikhl.gretty:gretty:1.1.7" // for the web container
    }
}

apply plugin: 'java'
apply plugin: 'war'
apply plugin: 'org.akhikhl.gretty'
apply plugin: 'project-report'
apply plugin: 'com.github.double16.java-saucelabs'

dependencies {
    functionalTestCompile 'com.github.double16:gradle-java-saucelabs-plugin:0.1-SNAPSHOT'
}

sauceLabs {
    account {
        username = System.getenv("SAUCE_LABS_USER")
        accessKey = System.getenv("SAUCE_LABS_ACCESS_PASSWORD")
    }
    connect {
        port = 4445
    }
}

browsers {
    firefox_linux_31
    firefox_windows_31
    chrome_mac
    chrome_windows_36
    delegate."internet explorer_Windows 7_11"
    delegate."internet explorer_Windows 7_10"
    delegate."internet explorer_vista_9"
    /*
    nexus4 {
        capabilities browserName: "android", platform: "Linux", version: "4.4", deviceName: "LG Nexus 4"
    }}
    */
    /* iOS doesn't work with sauce connect and internal names
    iphone {
        capabilities browserName: 'iPhone',            platform: 'OS X 10.9',   version: '7.1', 'device-orientation': 'portrait'
    }
    */
}
```


# JUnit

You should create an abstract base class that extends from com.github.double16.AbstractFunctionalTest to set common
parameters such as the base URL for your test server. However, the minimal that is required to create a test follows. The
files must be in the src/functionalTest/java folder.

```java
@RunWith(Parameterized.class)
public class MyFunctionalTest extends com.github.double16.AbstractFunctionalTest {
  MyFunctionalTest(WebDriver driver) {
    super(driver);
  }

  @Before
  public void setUp() throws Exception {
    baseUrl = "http://localhost:8080/myapp/";
    super.setUp();
  }

  @Test
  public void testTheThing() {
    driver.get(baseUrl+"index.do");
    report("first-page");
    ThingPage thingPage = PageFactory.initElements(driver, ThingPage.class);
    report("thing-page");
    Assert.assertTrue(...);
  }
}
```
