package com.azure.models.transportEvents;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class ChannelAcquisitionContextEventDeserializer extends StdDeserializer<ChannelAcquisitionContextEventBase> {

    protected ChannelAcquisitionContextEventDeserializer() {
        super(ChannelAcquisitionContextEventBase.class);
    }

    @Override
    public ChannelAcquisitionContextEventBase deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();

        if (node.get("completeNew") != null) {
            return p.getCodec().treeToValue(node, CompleteNewEvent.class);
        }
        if (node.get("pending") != null) {
            return p.getCodec().treeToValue(node, PendingEvent.class);
        }
        if (node.get("pendingTimeout") != null) {
            return p.getCodec().treeToValue(node, PendingTimeoutEvent.class);
        }
        if (node.get("poll") != null) {
            return p.getCodec().treeToValue(node, PollEvent.class);
        }
        if (node.get("startNew") != null) {
            return p.getCodec().treeToValue(node, StartNewEvent.class);
        }

        throw new IllegalStateException("Do not know how to process the node");
    }
}

