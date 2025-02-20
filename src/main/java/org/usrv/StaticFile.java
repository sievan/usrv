package org.usrv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class StaticFile {

    private final BufferedReader reader;

    private String fileContents;

    StaticFile(Path path) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(path.toString()));
    }

    String getFileContents() throws IOException {
        if (fileContents != null) {
            return fileContents;
        } else {
            String lines = reader.lines().collect(Collectors.joining("\n"));
            fileContents = lines;
            return lines;
        }
    }
}
