package dumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Json {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private Json() {}

    public static void write(Map<String, Object> data, Path outPath) throws IOException {
        Files.writeString(outPath, GSON.toJson(data));
    }
}
