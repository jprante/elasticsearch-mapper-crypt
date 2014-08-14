package org.xbib.elasticsearch.plugin.crypt;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.xbib.elasticsearch.action.crypt.KeyAction;
import org.xbib.elasticsearch.action.crypt.TransportKeyAction;
import org.xbib.elasticsearch.module.crypt.CryptIndexModule;
import org.xbib.elasticsearch.module.crypt.CryptModule;
import org.xbib.elasticsearch.index.mapper.crypt.CryptService;

import java.util.Collection;

import static org.elasticsearch.common.collect.Lists.newArrayList;

public class CryptPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "mapper-crypt-"
                + Build.getInstance().getVersion() + "-"
                + Build.getInstance().getShortHash();
    }

    @Override
    public String description() {
        return "Mapper plugin for crypted fields";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        modules.add(CryptModule.class);
        return modules;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = newArrayList();
        services.add(CryptService.class);
        return services;
    }

    @Override
    public Collection<Class<? extends Module>> indexModules() {
        Collection<Class<? extends Module>> modules = newArrayList();
        modules.add(CryptIndexModule.class);
        return modules;
    }

    public void onModule(ActionModule module) {
        module.registerAction(KeyAction.INSTANCE, TransportKeyAction.class);
    }

}
