package org.usrv;

public record ServerConfig(String distFolder, int port) {
    static ServerConfig getDefaultConfig() {
        return new ServerConfig("./dist", 80);
    }
}
