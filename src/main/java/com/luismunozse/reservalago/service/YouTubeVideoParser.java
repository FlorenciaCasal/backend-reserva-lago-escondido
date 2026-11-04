package com.luismunozse.reservalago.service;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class YouTubeVideoParser {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{6,20}$");

    public String normalizeVideoId(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Ingresá una URL de YouTube válida");
        }
        if (looksLikeHtml(trimmed)) {
            throw new ResponseStatusException(BAD_REQUEST, "Pegá solo el enlace de YouTube, no HTML ni iframe");
        }
        if (VIDEO_ID.matcher(trimmed).matches()) {
            return trimmed;
        }

        URI uri = parseUri(trimmed);
        String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
        String path = Optional.ofNullable(uri.getPath()).orElse("");

        if (host.equals("youtu.be") || host.equals("www.youtu.be")) {
            return validateId(firstPathSegment(path));
        }
        if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com")) {
            if (path.equals("/watch")) {
                return validateId(queryParam(uri.getRawQuery(), "v"));
            }
            if (path.startsWith("/embed/")) {
                return validateId(pathSegmentAfter(path, "/embed/"));
            }
            if (path.startsWith("/shorts/")) {
                return validateId(pathSegmentAfter(path, "/shorts/"));
            }
        }

        throw new ResponseStatusException(BAD_REQUEST, "La URL debe pertenecer a YouTube y apuntar a un video compatible");
    }

    public String embedUrl(String videoId) {
        return "https://www.youtube-nocookie.com/embed/" + validateId(videoId);
    }

    public String thumbnailUrl(String videoId) {
        return "https://img.youtube.com/vi/" + validateId(videoId) + "/hqdefault.jpg";
    }

    private boolean looksLikeHtml(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.contains("<") || lower.contains(">") || lower.contains("iframe") || lower.contains("script");
    }

    private URI parseUri(String value) {
        try {
            URI uri = new URI(value);
            if (uri.getScheme() == null || !(uri.getScheme().equals("https") || uri.getScheme().equals("http"))) {
                throw new ResponseStatusException(BAD_REQUEST, "La URL de YouTube debe comenzar con http o https");
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "La URL de YouTube no es válida");
        }
    }

    private String queryParam(String query, String name) {
        if (query == null || query.isBlank()) return null;
        return Arrays.stream(query.split("&"))
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2 && parts[0].equals(name))
                .map(parts -> parts[1])
                .findFirst()
                .orElse(null);
    }

    private String firstPathSegment(String path) {
        if (path == null) return null;
        return Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String pathSegmentAfter(String path, String prefix) {
        return firstPathSegment(path.substring(prefix.length() - 1));
    }

    private String validateId(String value) {
        String id = value == null ? "" : value.trim();
        if (!VIDEO_ID.matcher(id).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo identificar un ID de video de YouTube válido");
        }
        return id;
    }
}
