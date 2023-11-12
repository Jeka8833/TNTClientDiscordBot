package com.jeka8833.tntclientdiscord;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum ServerType {
    HYPIXEL("Hypixel", "Hypixel"),
    TNT_COMMUNITY("TNT Server", "Odyssey"),
    UNKNOWN("Unknown", "Unknown");

    private final String[] serverBrands;
    private final String displayName;

    ServerType(String displayName, String... serverBrands) {
        this.displayName = displayName;
        this.serverBrands = serverBrands;
    }

    @NotNull
    @Contract(pure = true)
    public String getActualServerBrand() {
        return serverBrands[0];
    }

    @NotNull
    @Contract(pure = true)
    public String getDisplayName() {
        return displayName;
    }

    public static ServerType getServer(String serverBrand) {
        for (ServerType type : ServerType.values()) {
            for (String brand : type.serverBrands) {
                if (brand.equalsIgnoreCase(serverBrand)) {
                    return type;
                }
            }
        }

        return UNKNOWN;
    }
}
