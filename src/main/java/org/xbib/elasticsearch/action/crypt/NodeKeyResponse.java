package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class NodeKeyResponse extends NodeOperationResponse {

    private String algorithm;

    private BytesReference encoded;

    private String format;

    private boolean destroyed;

    NodeKeyResponse(){}

    public NodeKeyResponse(DiscoveryNode node) {
        super(node);
    }

    public NodeKeyResponse setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public NodeKeyResponse setEncoded(byte[] encoded) {
        this.encoded = new BytesArray(encoded);
        return this;
    }

    public BytesReference getEncoded() {
        return encoded;
    }

    public NodeKeyResponse setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public NodeKeyResponse setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
        return this;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        algorithm = in.readString();
        format = in.readString();
        destroyed = in.readBoolean();
        encoded = in.readBytesReference();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(algorithm);
        out.writeString(format);
        out.writeBoolean(destroyed);
        out.writeBytesReference(encoded);
    }

    public String toString() {
        return "nodeId=" + getNode().name()
                + ",algorithm=" + algorithm
                + ",format=" + format
                + ",destroyed=" + destroyed
                + ",encoded=" + Base64.encodeBytes(encoded.toBytes());
    }
}
