package com.hfstudio.guidenh.network.capture;

import com.hfstudio.guidenh.client.capture.server.CaptureClientConnection;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class CaptureReplyHandler implements IMessageHandler<CaptureReplyMessage, IMessage> {

    @Override
    public IMessage onMessage(CaptureReplyMessage message, MessageContext context) {
        CaptureClientConnection.instance()
            .receive(context.netHandler, message.reply());
        return null;
    }
}
