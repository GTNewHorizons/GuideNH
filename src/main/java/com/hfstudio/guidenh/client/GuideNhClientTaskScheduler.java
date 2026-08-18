package com.hfstudio.guidenh.client;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientTaskScheduler {

    private static final Queue<Runnable> PENDING_TASKS = new ConcurrentLinkedQueue<>();
    private static final Runnable TICK_BOUNDARY = () -> {};
    private static boolean initialized;

    public static void initialize() {
        if (initialized) {
            return;
        }
        FMLCommonHandler.instance()
            .bus()
            .register(new GuideNhClientTaskScheduler());
        initialized = true;
    }

    public static boolean isOnClientThread() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && minecraft.func_152345_ab();
    }

    public static void execute(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (isOnClientThread()) {
            runTask(task);
            return;
        }
        PENDING_TASKS.add(task);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PENDING_TASKS.add(TICK_BOUNDARY);
        Runnable task;
        while ((task = PENDING_TASKS.poll()) != null && task != TICK_BOUNDARY) {
            runTask(task);
        }
    }

    private static void runTask(Runnable task) {
        try {
            task.run();
        } catch (Throwable error) {
            GuideDebugLog.error("Client task execution failed", error);
        }
    }
}
