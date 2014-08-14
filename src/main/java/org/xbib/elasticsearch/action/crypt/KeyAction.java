package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;

public class KeyAction extends Action<KeyRequest, KeyResponse, KeyRequestBuilder> {
    
    public final static KeyAction INSTANCE = new KeyAction();

    public final static String NAME = "org.xbib.elasticsearch.action.crypt";

    private KeyAction() {
        super(NAME);
    }

    @Override
    public KeyRequestBuilder newRequestBuilder(Client client) {
        return new KeyRequestBuilder(client);
    }

    @Override
    public KeyResponse newResponse() {
        return new KeyResponse();
    }
}
