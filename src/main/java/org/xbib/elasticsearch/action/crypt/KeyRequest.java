package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.support.nodes.NodesOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class KeyRequest extends NodesOperationRequest<KeyRequest> {

    private boolean createKey;

    public KeyRequest() {

    }

    public KeyRequest createKey(boolean createKey) {
        this.createKey = createKey;
        return this;
    }

    public boolean isCreateKey() {
        return createKey;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
