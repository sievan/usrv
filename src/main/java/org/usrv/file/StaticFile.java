package org.usrv.file;

import lombok.Getter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class StaticFile {

    private final String path;
    private static final TikaConfig config;

    static {
        try {
            config = new TikaConfig();
        } catch (TikaException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String fileContents;

    @Getter
    private final String mimeType;

    public StaticFile(Path path) throws IOException {
        this.path = path.toString();
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, this.path);

        mimeType = config.getDetector().detect(null, metadata).toString();
    }

    public String getFileContents() throws IOException {

        if (fileContents != null) {
            return fileContents;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String lines = reader.lines().collect(Collectors.joining("\n"));
            fileContents = lines;
            return lines;
        }
    }
}
