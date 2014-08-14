package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.support.nodes.NodeOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class NodeKeyRequest extends NodeOperationRequest {

    private boolean createKey;

    NodeKeyRequest() {
    }

    public NodeKeyRequest(String nodeId, KeyRequest request) {
        super(request, nodeId);
        createKey(request.isCreateKey());
    }

    public NodeKeyRequest createKey(boolean createKey) {
        this.createKey = createKey;
        return this;
    }

    public boolean isCreateKey() {
        return createKey;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        createKey = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBoolean(createKey);
    }
}
