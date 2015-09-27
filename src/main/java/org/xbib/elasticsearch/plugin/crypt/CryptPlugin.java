package org.xbib.elasticsearch.plugin.crypt;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.xbib.elasticsearch.module.crypt.CryptModule;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

public class CryptPlugin extends AbstractPlugin {

    private final Settings settings;

    @Inject
    public CryptPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "cryptmapper";
    }

    @Override
    public String description() {
        return "Crypt mapper";
    }

    @Override
    public Collection<Class<? extends Module>> indexModules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        if (settings.getAsBoolean("plugins.cryptmapper.enabled", true)) {
            modules.add(CryptModule.class);
        }
        return modules;
    }

}
