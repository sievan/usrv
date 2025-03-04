package org.usrv.file;

import lombok.Getter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFile {

    private final Path path;
    private static final TikaConfig config;

    static {
        try {
            config = new TikaConfig();
        } catch (TikaException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Getter
    private final String mimeType;

    public StaticFile(Path path) throws IOException {
        this.path = path;
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, this.path.toString());

        mimeType = config.getDetector().detect(null, metadata).toString();
    }

    public byte[] getFileContents() throws IOException {

        return Files.readAllBytes(this.path);
    }
}
