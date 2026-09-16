package com.hfstudio.guidenh.guide.compiler;

import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

/**
 * A single icon entry for navigation cycling. {@link #itemId} is the raw registry key string
 * (mod ID casing preserved) used for item lookup.
 */
public record NavigationIconEntry(String itemId, int meta, @Nullable NBTTagCompound nbt) {}
