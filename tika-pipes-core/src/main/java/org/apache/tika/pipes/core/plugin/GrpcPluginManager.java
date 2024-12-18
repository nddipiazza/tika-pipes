package org.apache.tika.pipes.core.plugin;

import java.nio.file.Path;
import java.util.List;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginLoader;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.apache.tika.pipes.core.exception.TikaPipesException;
import org.apache.tika.pipes.core.fetcher.Fetcher;

@Component
public class GrpcPluginManager extends DefaultPluginManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcPluginManager.class);

    public GrpcPluginManager() {
    }

    public GrpcPluginManager(Path... pluginsRoots) {
        super(pluginsRoots);
    }

    public GrpcPluginManager(List<Path> pluginsRoots) {
        super(pluginsRoots);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ClasspathPluginPropertiesFinder();
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return super.createPluginLoader();
    }

    @Override
    public void loadPlugins() {
        super.loadPlugins();
        LOGGER.info("Loaded {} plugins", getPlugins().size());
    }

    @Override
    public void startPlugins() {
        if (getPlugins().isEmpty()) {
            loadPlugins();
        }
        super.startPlugins();
        for (PluginWrapper plugin : getStartedPlugins()) {
            LOGGER.info("Add-in " + plugin.getPluginId() + " : " + plugin.getDescriptor() + " has started.");
            checkFetcherExtensions(plugin);
        }
    }

    private void checkFetcherExtensions(PluginWrapper plugin) {
        for (Class<?> extensionClass : getExtensionClasses(org.apache.tika.pipes.core.fetcher.Fetcher.class, plugin.getPluginId())) {
            if (!org.apache.tika.pipes.core.fetcher.Fetcher.class.isAssignableFrom(extensionClass)) {
                throw new TikaPipesException("Something is wrong with the classpath. " + Fetcher.class.getName() +
                        " should be assignable from " + extensionClass.getName() +
                        ". Did tika-core accidentally get in your plugin lib?");
            }
            LOGGER.info("    Extension " + extensionClass + " has been registered to plugin " + plugin.getPluginId());
        }
    }
}
