package org.xbib.elasticsearch.action.crypt;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.index.mapper.crypt.CryptService;

import javax.crypto.SecretKey;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TransportKeyAction extends TransportNodesOperationAction<KeyRequest, KeyResponse, NodeKeyRequest, NodeKeyResponse> {

    private final CryptService cryptService;

    @Inject
    public TransportKeyAction(Settings settings, ClusterName clusterName, ThreadPool threadPool,
                                       ClusterService clusterService, TransportService transportService,
                                       CryptService cryptService) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.cryptService = cryptService;
    }

    @Override
    protected String transportAction() {
        return KeyAction.NAME;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected KeyRequest newRequest() {
        return new KeyRequest();
    }

    @Override
    protected KeyResponse newResponse(KeyRequest request, AtomicReferenceArray nodesResponses) {
        KeyResponse response = new KeyResponse();
        for (int i = 0; i < nodesResponses.length(); i++) {
            Object nodesResponse = nodesResponses.get(i);
            if (nodesResponse instanceof NodeKeyResponse) {
                NodeKeyResponse nodeKeyResponse = (NodeKeyResponse) nodesResponse;
                response.add(nodeKeyResponse);
            }
        }
        return response;
    }

    @Override
    protected NodeKeyRequest newNodeRequest() {
        return new NodeKeyRequest();
    }

    @Override
    protected NodeKeyRequest newNodeRequest(String nodeId, KeyRequest request) {
        return new NodeKeyRequest(nodeId, request);
    }

    @Override
    protected NodeKeyResponse newNodeResponse() {
        return new NodeKeyResponse();
    }

    @Override
    protected NodeKeyResponse nodeOperation(NodeKeyRequest request) throws ElasticsearchException {
        NodeKeyResponse response = new NodeKeyResponse(clusterService.localNode());
        if (request.isCreateKey()) {
            SecretKey key = cryptService.generateKey();
            response.setAlgorithm(key.getAlgorithm())
                    .setEncoded(key.getEncoded())
                    .setFormat(key.getFormat())
                    .setDestroyed(key.isDestroyed());
        }
        return response;
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }
}
