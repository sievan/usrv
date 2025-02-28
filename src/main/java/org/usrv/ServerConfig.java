package org.usrv;

public record ServerConfig(String distFolder, int port, boolean serveSingleIndex) {
    static ServerConfig getDefaultConfig() {
        return new ServerConfig("./dist", 80, false);
    }
}
