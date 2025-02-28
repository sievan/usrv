package org.usrv.config;

public record ServerConfig(String distFolder, int port, boolean serveSingleIndex) {
    public static ServerConfig getDefaultConfig() {
        return new ServerConfig("./dist", 80, false);
    }
}
