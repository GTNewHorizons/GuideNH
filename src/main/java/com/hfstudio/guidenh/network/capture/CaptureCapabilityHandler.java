package com.hfstudio.guidenh.network.capture;

import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class CaptureCapabilityHandler implements IMessageHandler<CaptureCapabilityMessage, IMessage> {

    @Override
    public IMessage onMessage(CaptureCapabilityMessage message, MessageContext context) {
        CaptureClientConnection.instance()
            .capability(context.netHandler, message.version(), message.integrated());
        return null;
    }
}
