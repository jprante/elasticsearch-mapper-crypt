package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

public class KeyRequestBuilder extends NodesOperationRequestBuilder<KeyRequest, KeyResponse, KeyRequestBuilder> {

    public KeyRequestBuilder(Client client) {
        super((InternalGenericClient)client, new KeyRequest());
    }

    public KeyRequestBuilder createKey(boolean createKey) {
        request.createKey(createKey);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<KeyResponse> listener) {
        ((Client) client).execute(KeyAction.INSTANCE, request, listener);
    }
}
