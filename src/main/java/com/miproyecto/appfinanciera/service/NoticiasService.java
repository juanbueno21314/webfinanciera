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
import java.util.Set;

@Service
public class NoticiasService {

    private static final Logger log = LoggerFactory.getLogger(NoticiasService.class);

    private static final int MAX_NOTICIAS    = 6;
    private static final int MAX_POR_FUENTE  = 3;

    private static final String[] FALLBACKS = {
        "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=600&h=300&fit=crop&auto=format",
        "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&h=300&fit=crop&auto=format",
        "https://images.unsplash.com/photo-1579532537598-459ecdaf39cc?w=600&h=300&fit=crop&auto=format"
    };

    // Palabras clave que garantizan que el artículo sea de economía/finanzas
    private static final Set<String> KEYWORDS = Set.of(
        "economía", "economia", "económico", "economico", "económica", "economica",
        "finanzas", "financiero", "financiera", "financieros", "financieras",
        "dinero", "inversión", "inversion", "inversionista", "inversionistas",
        "bolsa", "acciones", "mercado", "mercados", "bursátil", "bursatil",
        "dólar", "dolar", "peso", "euro", "divisa", "divisas", "tasa de cambio",
        "banco", "bancos", "bancario", "bancaria", "banco central",
        "deuda", "crédito", "credito", "préstamo", "prestamo", "hipoteca",
        "inflación", "inflacion", "deflación", "deflacion",
        "pib", "gdp", "recesión", "recesion", "crecimiento económico",
        "empleo", "desempleo", "desocupación",
        "impuesto", "impuestos", "tributario", "tributaria", "reforma tributaria",
        "presupuesto", "fiscal", "déficit", "deficit", "superávit", "superavit",
        "ahorro", "pensión", "pension", "jubilación",
        "exportación", "exportacion", "importación", "importacion", "comercio exterior",
        "negocio", "negocios", "empresa", "empresas", "industria", "sector",
        "rentabilidad", "ganancia", "ganancias", "utilidad", "utilidades",
        "tasa", "interés", "interes", "rendimiento",
        "petróleo", "petroleo", "minería", "mineria", "commodities",
        "precio del", "precios del", "costo de", "salario", "salarios",
        "reservas", "emisión", "emision", "liquidez", "patrimonio",
        "fusión", "fusion", "adquisición", "adquisicion", "ipo", "capitalización"
    );

    // {nombre-fallback, url, "true"=aplicar filtro / "false"=ya es sección de economía}
    private static final List<String[]> FEEDS = List.of(
        new String[]{"Economía CO", "https://news.google.com/rss/search?q=economia+colombia+finanzas&hl=es-419&gl=CO&ceid=CO:es-419", "false"},
        new String[]{"Mercados CO", "https://news.google.com/rss/search?q=mercados+bolsa+dolar+inflacion+colombia&hl=es-419&gl=CO&ceid=CO:es-419", "false"}
    );

    private volatile List<NoticiaDto> cache = Collections.emptyList();

    @Scheduled(initialDelay = 5_000, fixedDelay = 1_800_000)
    public void cargarNoticias() {
        List<NoticiaDto> resultado = new ArrayList<>();
        for (String[] fuente : FEEDS) {
            if (resultado.size() >= MAX_NOTICIAS) break;
            String nombreFallback = fuente[0];
            String feedUrl       = fuente[1];
            boolean filtrar      = "true".equals(fuente[2]);
            int cargadasDeFuente = 0;
            try {
                SyndFeed feed = leerFeed(feedUrl);
                for (SyndEntry entry : feed.getEntries()) {
                    if (resultado.size() >= MAX_NOTICIAS) break;
                    if (cargadasDeFuente >= MAX_POR_FUENTE) break;

                    String titulo = entry.getTitle();
                    String enlace = entry.getLink();
                    if (titulo == null || enlace == null || titulo.isBlank()) continue;
                    if (filtrar && !esRelevante(titulo, entry)) continue;

                    // Google News añade " - Fuente" al final del título → separar
                    String fuente_real = nombreFallback;
                    int dashIdx = titulo.lastIndexOf(" - ");
                    if (dashIdx > 20) {
                        fuente_real = titulo.substring(dashIdx + 3).trim();
                        titulo = titulo.substring(0, dashIdx).trim();
                    }

                    String imagen = extraerImagen(entry, resultado.size());
                    resultado.add(new NoticiaDto(titulo, enlace, imagen, fuente_real));
                    cargadasDeFuente++;
                }
                log.info("RSS '{}': {} artículos cargados", nombreFallback, cargadasDeFuente);
            } catch (Exception e) {
                log.warn("No se pudo leer RSS '{}': {}", nombreFallback, e.getMessage());
            }
        }
        if (!resultado.isEmpty()) {
            cache = Collections.unmodifiableList(resultado);
        }
    }

    private boolean esRelevante(String titulo, SyndEntry entry) {
        StringBuilder texto = new StringBuilder(titulo.toLowerCase());
        if (entry.getCategories() != null) {
            entry.getCategories().forEach(c -> texto.append(' ').append(c.getName().toLowerCase()));
        }
        String textoFinal = texto.toString();
        return KEYWORDS.stream().anyMatch(textoFinal::contains);
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
