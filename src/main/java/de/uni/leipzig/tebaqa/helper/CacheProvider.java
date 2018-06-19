package de.uni.leipzig.tebaqa.helper;

import org.apache.log4j.Logger;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.springframework.core.io.ClassPathResource;

import java.util.HashMap;

class CacheProvider {

    private static Logger log = Logger.getLogger(CacheProvider.class);
    private static PersistentCacheManager cache;


    //do not instantiate
    private CacheProvider() {
    }

    /**
     * Provides a singleton instance of the PersistentCacheManager.
     *
     * @return A shared instance of the PersistentCacheManager.
     */
    static PersistentCacheManager getSingletonCacheInstance() {
        if (null == cache) {
            log.info("Creating cache...");
            cache = CacheManagerBuilder.newCacheManagerBuilder()
                    .with(CacheManagerBuilder.persistence(new ClassPathResource("coOccurrencePermutationCache").getPath()))
                    .withCache("persistent-cache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, HashMap.class,
                            ResourcePoolsBuilder.newResourcePoolsBuilder()
                                    .heap(1000, EntryUnit.ENTRIES)
                                    .disk(500, MemoryUnit.MB, true))
                    )
                    .build(true);

        }
        return cache;
    }
}
