package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.support.nodes.NodesOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.common.collect.Lists.newLinkedList;

public class KeyResponse extends NodesOperationResponse implements ToXContent {

    private List<NodeKeyResponse> keyResponseList = newLinkedList();

    public KeyResponse() {
    }

    public KeyResponse add(NodeKeyResponse nodeKeyResponse) {
        keyResponseList.add(nodeKeyResponse);
        return this;
    }

    public List<NodeKeyResponse> getKeyResponseList() {
        return keyResponseList;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        keyResponseList = newLinkedList();
        for (int i = 0; i < size; i++) {
            NodeKeyResponse nodeKeyResponse = new NodeKeyResponse();
            nodeKeyResponse.readFrom(in);
            keyResponseList.add(nodeKeyResponse);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(keyResponseList.size());
        for (NodeKeyResponse nodeKeyResponse: keyResponseList) {
            nodeKeyResponse.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        // TODO
        return builder;
    }

    public String toString() {
        return keyResponseList.toString();
    }
}
