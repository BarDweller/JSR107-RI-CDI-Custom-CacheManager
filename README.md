JCache Annotations for CDI(1.2+) in Bluemix.
--------------------------------------

This is a simple adapter layer to allow JSR107 Annotations to be used with
Redisson's JCache API (Could easily be adapted to others), configured automatically 
by the `rediscloud` from the `VCAP_SERVICES` environment var when hosted within Bluemix.

At the moment, this project supports only one rediscloud instance, if your app has multiple
rediscloud service instances attached, this project will use the first one it finds.

The code is pretty much entirely based from the Annotations & CDI layer from 
the JCache RI, at https://github.com/jsr107/RI. With a minor change to how the default 
CacheManager is configured, to have the default be built to use Redisson, configured 
via the VCAP_SERVICES env var.

Usage
-----

To use this project.. add it as a dependency to your application.. 

```
        <dependency>
            <groupId>com.github.BarDweller</groupId>
            <artifactId>JSR107-RI-CDI-Redisson-Bluemix</artifactId>
            <version>master-SNAPSHOT</version>
        </dependency>
```


Notes for Liberty
-----------------

If you are running on Liberty, you may need to exclude the root jar created by JitPack. 
It seems JitPack creates an empty zip file for a pom packaged project, and gives it the extension '.jar'
Jar files are supposed to have a META-INF/MANIFEST.MF entry, and a totally empty zip file isn't a valid jar.
Thankfully, empty zip files are unimportant to the application, so we'll use maven to remove the root jar.

```
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>2.6</version>
                <configuration>
                    <failOnMissingWebXml>false</failOnMissingWebXml>
                    <packagingExcludes>WEB-INF/lib/JSR107-RI-CDI-Redisson-Bluemix-master*,pom.xml</packagingExcludes>
                </configuration>
            </plugin>
``` 






