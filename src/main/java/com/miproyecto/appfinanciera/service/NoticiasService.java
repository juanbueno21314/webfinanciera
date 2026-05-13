package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.dto.NoticiaDto;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class NoticiasService {

    private static final Logger log = LoggerFactory.getLogger(NoticiasService.class);

    // Imágenes de respaldo distintas si el feed no trae imagen
    private static final String[] FALLBACKS = {
        "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=600&h=300&fit=crop&auto=format",
        "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&h=300&fit=crop&auto=format",
        "https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?w=600&h=300&fit=crop&auto=format"
    };

    private static final List<String[]> FEEDS = List.of(
        new String[]{"BBC Mundo",    "https://feeds.bbci.co.uk/mundo/economia/rss.xml"},
        new String[]{"Portafolio",   "https://www.portafolio.co/rss/portafolio.xml"},
        new String[]{"La República", "https://www.larepublica.co/rss/negocios"}
    );

    private volatile List<NoticiaDto> cache = Collections.emptyList();

    // initialDelay: espera 5s después de arrancar para no bloquear el inicio
    @Scheduled(initialDelay = 5_000, fixedDelay = 1_800_000)
    public void cargarNoticias() {
        List<NoticiaDto> resultado = new ArrayList<>();
        for (String[] fuente : FEEDS) {
            if (resultado.size() >= 3) break;
            String nombre = fuente[0];
            String feedUrl = fuente[1];
            try {
                SyndFeed feed = leerFeed(feedUrl);
                for (SyndEntry entry : feed.getEntries()) {
                    if (resultado.size() >= 3) break;
                    String titulo = entry.getTitle();
                    String enlace = entry.getLink();
                    if (titulo == null || enlace == null || titulo.isBlank()) continue;
                    String imagen = extraerImagen(entry, resultado.size());
                    resultado.add(new NoticiaDto(titulo.trim(), enlace, imagen, nombre));
                }
                log.info("RSS '{}': {} artículos cargados hasta ahora", nombre, resultado.size());
            } catch (Exception e) {
                log.warn("No se pudo leer RSS '{}': {}", nombre, e.getMessage());
            }
        }
        if (!resultado.isEmpty()) {
            cache = Collections.unmodifiableList(resultado);
        }
    }

    private SyndFeed leerFeed(String feedUrl) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(feedUrl).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Zenfi/1.0)");
        conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
        conn.setConnectTimeout(6_000);
        conn.setReadTimeout(10_000);
        try (InputStream is = conn.getInputStream()) {
            return new SyndFeedInput().build(new XmlReader(is));
        }
    }

    private String extraerImagen(SyndEntry entry, int index) {
        // 1) media:thumbnail — el más común en BBC, La República, Portafolio
        try {
            MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaModule.URI);
            if (media != null && media.getMetadata() != null) {
                Thumbnail[] thumbs = media.getMetadata().getThumbnail();
                if (thumbs != null && thumbs.length > 0 && thumbs[0].getUrl() != null) {
                    return thumbs[0].getUrl().toString();
                }
            }
            // 2) media:content con referencia de imagen
            if (media != null && media.getMediaContents() != null) {
                for (var content : media.getMediaContents()) {
                    if (content.getReference() != null) {
                        String ref = content.getReference().toString();
                        if (ref.matches(".*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$")) return ref;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3) Enclosure de tipo imagen
        if (entry.getEnclosures() != null) {
            for (SyndEnclosure enc : entry.getEnclosures()) {
                if (enc.getType() != null && enc.getType().startsWith("image/") && enc.getUrl() != null) {
                    return enc.getUrl();
                }
            }
        }

        // 4) <img> dentro del HTML del description
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            org.jsoup.nodes.Element img = Jsoup.parse(entry.getDescription().getValue()).selectFirst("img");
            if (img != null) {
                String src = img.attr("src");
                if (!src.isBlank()) return src;
            }
        }

        // 5) Fallback único por posición
        return FALLBACKS[index % FALLBACKS.length];
    }

    public List<NoticiaDto> obtenerNoticias() {
        return cache; // retorna lo que haya en caché (vacío al inicio, se llena tras 5s)
    }

    public NoticiaDto obtenerUltimaNoticia() {
        return cache.isEmpty() ? null : cache.get(0);
    }
}
