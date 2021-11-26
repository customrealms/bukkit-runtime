package io.customrealms.resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class Resource {

    /**
     * Reads the contents of a resource file to a string
     * @param filename the filename relative to the resources
     * @return the string contents of the file, or null
     */
    public static String read(String filename) {
        return new Resource(filename).getStringContents();
    }

    /**
     * The filename of the resource in the archive
     */
    private final String filename;

    /**
     * Constructs a new resource object
     * @param filename the filename of the resource within the JAR file
     */
    public Resource(String filename) {
        this.filename = filename;
    }

    /**
     * Gets the string contents of the resource file
     * @return the string contents
     */
    public String getStringContents() {

        // Get the stream for the path
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(this.filename);

        // If the input stream is null
        if (inputStream == null) return null;

        // Write the contents to a string writer
        return Resource.streamToString(inputStream);

    }

    private static String streamToString(InputStream inputStream) {

        try {

            // Create the result output stream
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            // Create a buffer to load data into
            byte[] buffer = new byte[1024];

            // While we're pulling some data
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            // Convert it to a string
            return result.toString("UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

    }

}
