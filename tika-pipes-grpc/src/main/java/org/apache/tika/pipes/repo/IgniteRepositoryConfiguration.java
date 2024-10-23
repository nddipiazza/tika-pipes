package org.apache.tika.pipes.repo;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.springdata.repository.config.EnableIgniteRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.tika.pipes.fetcher.FetcherConfig;

@Configuration
@EnableIgniteRepositories
public class IgniteRepositoryConfiguration {
    @Bean
    public Ignite igniteInstance() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("springDataNode");
        cfg.setPeerClassLoadingEnabled(true);

        CacheConfiguration<String, FetcherConfig> ccfg = new CacheConfiguration<>("FetcherCache");
        ccfg.setIndexedTypes(String.class, FetcherConfig.class);
        cfg.setCacheConfiguration(ccfg);

        return Ignition.start(cfg);
    }
}
