package org.xbib.elasticsearch.module.crypt;

import org.elasticsearch.common.inject.AbstractModule;
import org.xbib.elasticsearch.index.mapper.crypt.CryptService;

public class CryptModule extends AbstractModule {

    @Override
    public void configure() {
        bind(CryptService.class).asEagerSingleton();
    }
}
