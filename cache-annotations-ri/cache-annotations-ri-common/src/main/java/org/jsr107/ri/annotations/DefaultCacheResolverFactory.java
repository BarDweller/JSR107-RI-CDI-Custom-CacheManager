/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jsr107.ri.annotations;

import java.io.StringReader;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.jcache.JCacheManager;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Default {@link CacheResolverFactory} that uses the default {@link CacheManager} and finds the {@link Cache}
 * using {@link CacheManager#getCache(String)}. Returns a {@link DefaultCacheResolver} that wraps the found
 * {@link Cache}
 *
 * @author Eric Dalquist
 * @author Rick Hightower
 * @since 1.0
 */
public class DefaultCacheResolverFactory implements CacheResolverFactory {
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final CacheManager cacheManager;
  private final CacheConfigCustomizer configCustomizer;
  
  /**
   * Customize the config used to create caches via this resolver factory.
   */
  public interface CacheConfigCustomizer{
     public customizeConfiguration(MutableConfiguration<Object, Object> config);
  }
  
  /**
   * Constructs the resolver
   *
   * @param cacheManager the cache manager to use
   */
  public DefaultCacheResolverFactory(CacheManager cacheManager, CacheConfigCustomizer configCustomizer) {
    this.cacheManager = cacheManager;
    this.configCustomizer = configCustomizer;
  }
  
  /**
   * Constructs the resolver
   *
   * @param cacheManager the cache manager to use
   */
  public DefaultCacheResolverFactory(CacheManager cacheManager) {
    this(cacheManager,null);
  }
  
  //static .. for now.
  private static RedissonClient redisson = null;

  /**
   * Constructs the resolver
   */
  public DefaultCacheResolverFactory() {
    CacheManager result = null;

    //Attempt to configure redisson from vcap_Services
    String vcap_services = System.getenv("VCAP_SERVICES");
    if( vcap_services != null && vcap_services.length()>0 ){
          try{
              //read the vcap_services
              JsonReader reader = Json.createReader(new StringReader(vcap_services));
              JsonObject root = reader.readObject();
              //get back info for 'rediscloud' service, we only expect one, but 
              //vcap allows for multiple, so it's an array..
              JsonArray rediscloud = root.getJsonArray("rediscloud");
              //possible the service isn't bound?
              if(rediscloud!=null){
                  //if we had 2 different rediscloud services bound to this
                  //app, then we'd need to differentiate between them here.
                  //but thankfully, we can just use the one and only =)
                  JsonObject instance = rediscloud.getJsonObject(0);
                  
                  //with the service being there, grab all the connection info.. 
                  JsonObject creds = instance.getJsonObject("credentials");
                  String port = creds.getString("port");
                  String host = creds.getString("hostname");
                  String pwd  = creds.getString("password");
                  
                  logger.info("Using Redis server at "+host+":"+port);

                  //Build a direct redisson config for the bound redis service
                  Config redissonConfig = new Config();
                  redissonConfig.useSingleServer().setAddress(host+":"+port).setPassword(pwd);

                  //TODO: this approach is temporary until Redisson 3.2.4 is released with 
                  //      improved programmatic configuration support.
                  
                  //Configure a JCache manager using that redisson config.
                  if(redisson != null){
                    redisson = Redisson.create(redissonConfig);
                  }

                  //Should probably close the manager, but that fails at the mo, because we build it with
                  //a null provider.. will get resolved with the 3.2.4 changes pending.
                  @SuppressWarnings("resource")
                  CacheManager manager = new JCacheManager((Redisson)redisson, JCacheManager.class.getClassLoader(), null, null, null);
                  
                  result = manager;
              }else{
                  logger.info("vcap_services was missing the rediscloud entry, is the service bound?");
              }
              
              reader.close();
          }catch(Exception e){
              //for now.. a generic catch all to prevent the errors being hidden by cdi during init.. 
              logger.log(Level.SEVERE,"Caught Exception during vcap services processing ", e);
          }
    }

    if(result == null){
      logger.info("Using default CacheManager");
      CachingProvider provider = Caching.getCachingProvider();
      this.cacheManager = provider.getCacheManager(provider.getDefaultURI(), provider.getDefaultClassLoader());
    }else{
      logger.info("Using vcap_services configured CacheManager"); 
      this.cacheManager = result;
    }
  }
  
  /* (non-Javadoc)
   * @see javax.cache.annotation.CacheResolverFactory#getCacheResolver(javax.cache.annotation.CacheMethodDetails)
   */
  @Override
  public CacheResolver getCacheResolver(CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
    final String cacheName = cacheMethodDetails.getCacheName();
    Cache<?, ?> cache = this.cacheManager.getCache(cacheName);

    if (cache == null) {
      logger.warning("No Cache named '" + cacheName + "' was found in the CacheManager, a default cache will be created.");
      
      MutableConfiguration<Object, Object>() config = new MutableConfiguration<Object, Object>();
      if(configCustomizer != null){
        configCustomizer.customizeConfiguration(config);
      }
      
      cacheManager.createCache(cacheName, config);
      cache = cacheManager.getCache(cacheName);
    }

    return new DefaultCacheResolver(cache);
  }

  @Override
  public CacheResolver getExceptionCacheResolver(CacheMethodDetails<CacheResult> cacheMethodDetails) {
    final CacheResult cacheResultAnnotation = cacheMethodDetails.getCacheAnnotation();
    final String exceptionCacheName = cacheResultAnnotation.exceptionCacheName();
    if (exceptionCacheName == null || exceptionCacheName.trim().length() == 0) {
      throw new IllegalArgumentException("Can only be called when CacheResult.exceptionCacheName() is specified");
    }

    Cache<?, ?> cache = cacheManager.getCache(exceptionCacheName);

    if (cache == null) {
      logger.warning("No Cache named '" + exceptionCacheName +
          "' was found in the CacheManager, a default cache will be created.");
      
      MutableConfiguration<Object, Object>() config = new MutableConfiguration<Object, Object>();
      if(configCustomizer != null){
        configCustomizer.customizeConfiguration(config);
      }
      
      cacheManager.createCache(cacexceptionCacheName, config);
      cache = cacheManager.getCache(exceptionCacheName);
    }

    return new DefaultCacheResolver(cache);
  }
}
