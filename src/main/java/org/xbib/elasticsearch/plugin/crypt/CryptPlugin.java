package org.xbib.elasticsearch.plugin.crypt;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.xbib.elasticsearch.module.crypt.CryptModule;

import java.util.ArrayList;
import java.util.Collection;


public class CryptPlugin extends Plugin {

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
    public Collection<Module> indexModules(Settings indexSettings) {
        Collection<Module> modules = new ArrayList<>();
        if (settings.getAsBoolean("plugins.cryptmapper.enabled", true)) {
            modules.add(new CryptModule());
        }
        return modules;
    }

}
