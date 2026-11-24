package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerHorseInventory;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.MathHelper;

import com.hfstudio.guidenh.capture.server.CaptureContainerInventories;
import com.hfstudio.guidenh.capture.server.api.CaptureGuiLocation;
import com.hfstudio.guidenh.capture.server.api.CaptureServerContext;
import com.hfstudio.guidenh.capture.server.api.CaptureServerGuiAction;
import com.hfstudio.guidenh.capture.server.api.CaptureServerObjectProvider;
import com.hfstudio.guidenh.capture.server.api.CaptureServerObjectTarget;
import com.hfstudio.guidenh.capture.server.api.CaptureServerTarget;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorCaptureHorseContainer;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorCaptureMerchantContainer;

public class VanillaEntityGuiProvider implements CaptureServerObjectProvider {

    @Override
    public String id() {
        return "vanilla_entity";
    }

    @Override
    public List<CaptureServerObjectTarget> enumerate(CaptureServerContext context) {
        List<CaptureServerObjectTarget> targets = new ArrayList<>();
        for (Object value : context.world().loadedEntityList) {
            Entity entity = (Entity) value;
            if (!(entity instanceof EntityHorse || entity instanceof EntityVillager
                || entity instanceof EntityMinecartChest
                || entity instanceof EntityMinecartHopper)) continue;
            if (!context.query()
                .includes(
                    entity.posX,
                    entity.posY,
                    entity.posZ,
                    context.player().posX,
                    context.player().posY,
                    context.player().posZ))
                continue;
            EntityAction action = new EntityAction(entity);
            CaptureGuiLocation location = action.location(context);
            String reason = action.accessReason(context);
            if (reason.isEmpty() && !context.query()
                .temporaryTravel() && entity.getDistanceSqToEntity(context.player()) >= 64)
                reason = "Outside interaction distance";
            CaptureServerTarget target = new CaptureServerTarget(
                id() + ":" + entity.getUniqueID(),
                id(),
                location.dimension(),
                location.x(),
                location.y(),
                location.z(),
                "main",
                entity.getCommandSenderName(),
                reason.isEmpty() ? "ready" : "unavailable",
                reason);
            targets.add(new CaptureServerObjectTarget(target, action));
            if (targets.size() >= context.query()
                .maxTargets()) break;
        }
        return List.copyOf(targets);
    }

    public static class EntityAction implements CaptureServerGuiAction {

        private final Entity entity;

        public EntityAction(Entity entity) {
            this.entity = entity;
        }

        @Override
        public CaptureGuiLocation location(CaptureServerContext context) {
            return new CaptureGuiLocation(
                entity.dimension,
                MathHelper.floor_double(entity.posX),
                MathHelper.floor_double(entity.posY),
                MathHelper.floor_double(entity.posZ));
        }

        @Override
        public String accessReason(CaptureServerContext context) {
            if (entity.isDead || entity.worldObj != context.world()
                || context.world()
                    .getEntityByID(entity.getEntityId()) != entity)
                return "The entity disappeared";
            if (entity instanceof EntityHorse horse) {
                if (!horse.isTame() || !horse.isAdultHorse()) return "A tamed adult horse is required";
                if (horse.riddenByEntity != null && horse.riddenByEntity != context.player())
                    return "The horse is in use";
            }
            if (entity instanceof EntityVillager villager) {
                if (villager.isChild()) return "A grown villager is required";
                if (villager.getCustomer() != null && villager.getCustomer() != context.player())
                    return "The villager is trading with another player";
            }
            return "";
        }

        @Override
        public void open(CaptureServerContext context) {
            String reason = accessReason(context);
            if (!reason.isEmpty()) throw new IllegalStateException(reason);
            if (entity instanceof EntityHorse horse) horse.openGUI(context.player());
            else if (entity instanceof EntityVillager villager) {
                villager.setCustomer(context.player());
                try {
                    context.player()
                        .displayGUIMerchant(villager, villager.getCustomNameTag());
                    if (!owns(context, context.player().openContainer))
                        throw new IllegalStateException("The villager trading window did not open");
                } catch (RuntimeException error) {
                    if (villager.getCustomer() == context.player()) villager.setCustomer(null);
                    throw error;
                }
            } else if (entity instanceof IInventory inventory) context.player()
                .displayGUIChest(inventory);
        }

        @Override
        public boolean owns(CaptureServerContext context, Container container) {
            if (entity instanceof EntityHorse) return container instanceof ContainerHorseInventory
                && ((AccessorCaptureHorseContainer) container).guidenh$captureHorse() == entity;
            if (entity instanceof EntityVillager) return container instanceof ContainerMerchant
                && ((AccessorCaptureMerchantContainer) container).guidenh$captureMerchant() == entity;
            return entity instanceof IInventory inventory && CaptureContainerInventories.contains(container, inventory);
        }
    }
}
