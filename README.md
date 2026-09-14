High performance block tracking solution for your server.

This fork has remade and simplified the original plugin a lot. It's almost a completely new plugin.

Using BlockTracker in your plugin
------
##### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>dev.krakenied</groupId>
    <artifactId>blocktracker</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```
##### Gradle
```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io/")
    }
}
```
```kotlin
dependencies {
    compileOnly("dev.krakenied:blocktracker:1.0.7")
}
```
