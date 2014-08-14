package org.xbib.elasticsearch.module.crypt;

import org.elasticsearch.common.inject.AbstractModule;

public class CryptIndexModule extends AbstractModule {

    @Override
    public void configure() {
        bind(RegisterCryptType.class).asEagerSingleton();
    }
}
