package net.optifine.http;

import java.util.Map;

public record HttpResponse(int status, String statusLine, Map<String, String> headers, byte[] body) {

    public String getHeader(String key) {
        return this.headers.get(key);
    }
}
