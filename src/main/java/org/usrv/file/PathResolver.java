package org.usrv.file;


import org.usrv.config.ServerConfig;
import org.usrv.http.ClientRequest;

import java.nio.file.Path;

public class PathResolver {
    private final ServerConfig serverConfig;

    public PathResolver(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Path resolveRequest(ClientRequest request) {
        String pathStr = request.path();
        String[] splitPath = request.uri().getPath().split("/");
        boolean containsFileExtension;

        if (splitPath.length == 0) {
            containsFileExtension = pathStr.contains(".");
        } else {
            containsFileExtension = splitPath[splitPath.length - 1].contains(".");
        }

        boolean clientWantsHtml = request.headers().get("Accept") == null ||
                request.headers().get("Accept").contains("text/html") ||
                request.headers().get("Accept").contains("*/*");

        if (!containsFileExtension && clientWantsHtml) {
            if (serverConfig.serveSingleIndex()) {
                // In SPA mode, all HTML requests go to index.html
                pathStr = "/index.html";
            } else if (request.path().endsWith("/")) {
                // In standard mode, append index.html to directory paths
                pathStr += "index.html";
            } else {
                // Optional: handle non-directory paths without extensions as directories
                pathStr += "/index.html";
            }
        }

        return Path.of(serverConfig.distFolder(), pathStr);
    }
}
