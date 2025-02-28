package org.usrv;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(new ServerConfig("./dist", 80, true));
        server.start();
    }
}