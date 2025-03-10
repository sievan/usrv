package org.usrv.file;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaticFileTests {

    @Test
    @DisplayName("You should be able to get the contents of a file")
    void testGetFileContents() throws IOException {

        String filePath = "./";

        File file = File.createTempFile("test", ".html", new File(filePath));
        file.deleteOnExit();

        FileWriter writer = new FileWriter(file);

        String randomizedContent = String.format("<html>%s</html>", UUID.randomUUID());

        writer.write(randomizedContent);
        writer.close();

        StaticFile sFile = new StaticFile(Path.of(file.getPath()));

        byte[] fileContents = sFile.getFileContents();

        assertArrayEquals(randomizedContent.getBytes(), fileContents);
    }

    @Test
    @DisplayName("You should be able to get the contents of a nested file")
    void testGetNestedFileContents() throws IOException {

        String folderPath = "./TEST_DIST";
        String filePath = "./TEST_DIST/";

        File folder = new File(folderPath);
        folder.mkdir();
        folder.deleteOnExit();

        File tmpFile = File.createTempFile("test", ".html", new File(filePath));
        tmpFile.deleteOnExit();

        FileWriter writer = new FileWriter(tmpFile);

        String randomizedContent = String.format("<html>%s</html>", UUID.randomUUID());

        writer.write(randomizedContent);
        writer.close();

        StaticFile sFile = new StaticFile(Path.of(tmpFile.getPath()));

        byte[] fileContents = sFile.getFileContents();

        assertArrayEquals(randomizedContent.getBytes(), fileContents);
    }

    @Test
    @DisplayName("You should be able to get the mime types from files")
    void testGetMimeType() throws IOException {

        String filePath = "./";

        Stream<String[]> fileStream = Stream.of(
                new String[]{".svg", "image/svg+xml"},
                new String[]{".htm", "text/html"},
                new String[]{".html", "text/html"},
                new String[]{".jpg", "image/jpeg"},
                new String[]{".jpeg", "image/jpeg"},
                new String[]{".jpe", "image/jpeg"}
        );

        fileStream.forEach(entry -> {
            String fileContent = "Content";
            try {
                File file = File.createTempFile("test", entry[0], new File(filePath));
                file.deleteOnExit();

                StaticFile sFile = new StaticFile(Path.of(file.getPath()));

                assertEquals(entry[1], sFile.getMimeType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


    }
}