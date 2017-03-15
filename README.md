[![Release](https://jitpack.io/v/BarDweller/JSR107-RI-CDI-Custom-CacheManager.svg)](https://jitpack.io/#BarDweller/JSR107-RI-CDI-Custom-CacheManager)

Configurable JCache Annotations
-------------------------------

This is a simple fork of the RI annations layer to allow JSR107 Annotations to be used with
CDI Containers, with the additional capability that the default CacheManager can be supplied
via a java service approach, rather than coming from the usual 

```
Caching.getCachingProvider().getCacheManager()
```

This allows for the CacheManager to be instantiated using config gathered programatically, 
and allows the application to pick & instantiate the backing JSR107 CacheManager implementation
that should be used by the annotations.

The code is pretty much entirely the same as the Annotations & CDI layer from 
the JCache RI, at https://github.com/jsr107/RI. With a minor change to how the default 
CacheManager is configured, to allow the default CacheManager to come from a java
service implementation, and a slight change to the CDI interceptor definitions to allow
applications to depend on this repo as a jitpack library, with only an empty beans.xml.

Usage
-----

To use this project.. add it as a dependency to your application.. 

```
        <dependency>
            <groupId>com.github.BarDweller</groupId>
            <artifactId>JSR107-RI-CDI-Custom-CacheManager</artifactId>
            <version>v1.0.9-STILLETO</version>
        </dependency>
```

And add an empty `beans.xml` to your `WEB-INF` folder.

To customise the CacheManager used by annotations, simply add a java services file with the name;
```
META-INF/services/org.jsr107.ri.annotations.DefaultCacheResolverFactory$DefaultCacheManagerProvider
```
And include in that file, the name of the implementation class for the `DefaultCacheManagerProvider` you wish the annotations
code to use. 

`org.jsr107.ri.annotations.DefaultCacheResolverFactory.DefaultCacheManagerProvider` is a simple interface 
with a single method;
```
  public interface DefaultCacheManagerProvider {
    public CacheManager getDefaultCacheManager();
  }
```

You could choose to say, parse VCAP_SERVICES within a cloud environment, and then instantiate an appropriately 
configured [Redisson](https://redisson.org/) instance of JCacheManager, [like this](https://github.com/BarDweller/gameon-jsr107-room/blob/master/src/main/java/org/gameontext/sample/jsr107defaultprovider/RedissonCacheManagerProvider.java)







