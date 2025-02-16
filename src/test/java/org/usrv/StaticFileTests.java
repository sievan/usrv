package org.usrv;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StaticFileTests {

    @Test
    void testGetFileData() throws IOException {

        File file = File.createTempFile("tst", ".html", new File("./"));
        file.deleteOnExit();

        FileWriter writer = new FileWriter(file);

        writer.write("<html>Test</html>");
        writer.close();

        FileReader fReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fReader);

        assertEquals("<html>Test</html>", reader.readLine());

        file.delete();
    }

    @Test
    void testGetFileContents() throws IOException {

        String filePath = "./";

        File file = File.createTempFile("test", ".html", new File(filePath));
        file.deleteOnExit();

        FileWriter writer = new FileWriter(file);

        String randomizedContent = String.format("<html>%s</html>", UUID.randomUUID());

        writer.write(randomizedContent);
        writer.close();

        StaticFile sFile = new StaticFile(file.getPath());

        String fileContents = sFile.getFileContents();

        assertEquals(randomizedContent, fileContents);

        file.delete();
    }
}