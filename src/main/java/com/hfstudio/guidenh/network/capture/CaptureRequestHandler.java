package com.hfstudio.guidenh.network.capture;

import com.hfstudio.guidenh.capture.server.CaptureServerCoordinator;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class CaptureRequestHandler implements IMessageHandler<CaptureRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(CaptureRequestMessage message, MessageContext context) {
        CaptureServerCoordinator.instance()
            .enqueue(context.getServerHandler().playerEntity.getUniqueID(), message.request());
        return null;
    }
}
