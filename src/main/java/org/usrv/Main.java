package org.usrv;

import org.usrv.config.ServerConfig;
import org.usrv.http.Server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(new ServerConfig("./dist", 80, false));
        server.start();
    }
}