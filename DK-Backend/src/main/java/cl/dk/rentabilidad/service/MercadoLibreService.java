package cl.dk.rentabilidad.service;

import cl.dk.rentabilidad.dto.MercadoLibreCostoDto;
import cl.dk.rentabilidad.dto.MercadoLibreImportResultDto;
import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.CostoVenta;
import cl.dk.rentabilidad.entity.MercadoLibreCosto;
import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.exception.ResourceNotFoundException;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.CostoVentaRepository;
import cl.dk.rentabilidad.repository.MercadoLibreCostoRepository;
import cl.dk.rentabilidad.repository.ProductoRepository;
import cl.dk.rentabilidad.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MercadoLibreService {

    private static final String CANAL_MERCADO_LIBRE = "MercadoLibre";

    private final MercadoLibreCostoRepository mercadoLibreCostoRepository;
    private final ProductoRepository productoRepository;
    private final CanalVentaRepository canalVentaRepository;
    private final VentaRepository ventaRepository;
    private final CostoVentaRepository costoVentaRepository;
    private final RentabilidadService rentabilidadService;

    @Transactional
    public MercadoLibreImportResultDto importarDesdeCsv(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV está vacío");
        }

        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("El CSV no tiene encabezados");
            }

            char separador = detectarSeparador(headerLine);
            List<String> headers = parsearLinea(headerLine, separador);
            Map<String, Integer> columnas = mapearColumnas(headers);

            String linea;
            int fila = 1;
            while ((linea = reader.readLine()) != null) {
                fila++;
                if (linea.isBlank()) {
                    continue;
                }

                try {
                    List<String> valores = parsearLinea(linea, separador);
                    ResultadoFila resultado = procesarFila(valores, columnas, archivo.getOriginalFilename());
                    switch (resultado) {
                        case CREADO -> creados++;
                        case ACTUALIZADO -> actualizados++;
                        case OMITIDO -> omitidos++;
                    }
                } catch (Exception e) {
                    errores++;
                    detalleErrores.add("Fila " + fila + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el CSV de MercadoLibre: " + e.getMessage(), e);
        }

        return MercadoLibreImportResultDto.builder()
                .creados(creados)
                .actualizados(actualizados)
                .omitidos(omitidos)
                .errores(errores)
                .totalProcesados(creados + actualizados + omitidos)
                .importadoEn(LocalDateTime.now())
                .detalleErrores(detalleErrores.stream().limit(50).toList())
                .build();
    }

    @Transactional
    public MercadoLibreImportResultDto importarVentasDesdeCsv(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV de ventas de MercadoLibre está vacío");
        }

        CanalVenta canal = canalVentaRepository.findByNombre(CANAL_MERCADO_LIBRE)
                .orElseThrow(() -> new IllegalStateException("No existe el canal 'MercadoLibre' en la base"));

        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        int errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("El CSV de ventas no tiene encabezados");
            }

            char separador = detectarSeparador(headerLine);
            List<String> headers = parsearLinea(headerLine, separador);
            Map<String, Integer> columnas = mapearColumnasVentas(headers);

            String linea;
            int fila = 1;
            while ((linea = reader.readLine()) != null) {
                fila++;
                if (linea.isBlank()) {
                    continue;
                }

                try {
                    List<String> valores = parsearLinea(linea, separador);
                    ResultadoFila resultado = procesarFilaVenta(valores, columnas, canal);
                    switch (resultado) {
                        case CREADO -> creados++;
                        case ACTUALIZADO -> actualizados++;
                        case OMITIDO -> omitidos++;
                    }
                } catch (Exception e) {
                    errores++;
                    detalleErrores.add("Fila " + fila + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el CSV de ventas de MercadoLibre: " + e.getMessage(), e);
        }

        return MercadoLibreImportResultDto.builder()
                .creados(creados)
                .actualizados(actualizados)
                .omitidos(omitidos)
                .errores(errores)
                .totalProcesados(creados + actualizados + omitidos)
                .importadoEn(LocalDateTime.now())
                .detalleErrores(detalleErrores.stream().limit(50).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MercadoLibreCostoDto> listar() {
        return mercadoLibreCostoRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MercadoLibreCostoDto obtenerPorSku(String sku) {
        MercadoLibreCosto costo = mercadoLibreCostoRepository.findBySku(sku.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No existe costo de MercadoLibre para SKU: " + sku));
        return toDto(costo);
    }

    @Transactional(readOnly = true)
    public String exportarCsv() {
        String header = "SKU,CostoProm,UltimoCosto,CostoMercadoLibre";
        String body = mercadoLibreCostoRepository.findAll().stream()
                .sorted((a, b) -> a.getSku().compareToIgnoreCase(b.getSku()))
                .map(c -> String.join(",",
                        escaparCsv(c.getSku()),
                        formatearNumero(c.getCostoProm()),
                        formatearNumero(c.getUltimoCosto()),
                        formatearNumero(c.getCostoMercadoLibre())))
                .collect(Collectors.joining("\n"));

        if (body.isBlank()) {
            return header + "\n";
        }
        return header + "\n" + body + "\n";
    }

    private ResultadoFila procesarFila(List<String> valores, Map<String, Integer> columnas, String fuenteArchivo) {
        String sku = valor(valores, columnas, "sku").trim();
        if (sku.isBlank()) {
            return ResultadoFila.OMITIDO;
        }

        BigDecimal costoMercadoLibre = parsearNumero(valor(valores, columnas, "costo_mercadolibre"));
        BigDecimal costoProm = parsearNumero(valor(valores, columnas, "costo_prom"));
        BigDecimal ultimoCosto = parsearNumero(valor(valores, columnas, "ultimo_costo"));

        if (costoMercadoLibre == null) {
            costoMercadoLibre = ultimoCosto != null && ultimoCosto.signum() > 0
                    ? ultimoCosto
                    : costoProm;
        }

        if (costoMercadoLibre == null || costoMercadoLibre.signum() <= 0) {
            return ResultadoFila.OMITIDO;
        }

        Optional<MercadoLibreCosto> existenteOpt = mercadoLibreCostoRepository.findBySku(sku);
        MercadoLibreCosto costo = existenteOpt.orElseGet(MercadoLibreCosto::new);

        costo.setSku(sku);
        costo.setCostoProm(costoProm);
        costo.setUltimoCosto(ultimoCosto);
        costo.setCostoMercadoLibre(costoMercadoLibre.setScale(2, RoundingMode.HALF_UP));
        costo.setFuenteArchivo(fuenteArchivo != null ? fuenteArchivo : "mercadolibre.csv");

        mercadoLibreCostoRepository.save(costo);
        actualizarCostoBaseProducto(sku, costo.getCostoMercadoLibre());
        return existenteOpt.isPresent() ? ResultadoFila.ACTUALIZADO : ResultadoFila.CREADO;
    }

    private ResultadoFila procesarFilaVenta(List<String> valores, Map<String, Integer> columnas, CanalVenta canal) {
        String orderId = valor(valores, columnas, "orderid").trim();
        String sku = valor(valores, columnas, "sku").trim();
        if (orderId.isBlank() || sku.isBlank()) {
            return ResultadoFila.OMITIDO;
        }

        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU no existe en catálogo: " + sku));

        BigDecimal precioVenta = parsearNumero(valor(valores, columnas, "precioventa"));
        if (precioVenta == null) {
            throw new IllegalArgumentException("Falta precioVenta para orderId " + orderId);
        }

        Integer cantidad = parsearEntero(valor(valores, columnas, "cantidad"));
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida para orderId " + orderId);
        }

        LocalDate fechaVenta = parsearFecha(valor(valores, columnas, "fecha"));
        if (fechaVenta == null) {
            throw new IllegalArgumentException("Fecha inválida para orderId " + orderId);
        }

        BigDecimal comision = parsearNumero(valor(valores, columnas, "comision"));
        BigDecimal envio = parsearNumero(valor(valores, columnas, "envio"));

        Venta ventaExistente = ventaRepository.findByReferenciaExterna(orderId).orElse(null);
        Venta venta = ventaExistente != null ? ventaExistente : new Venta();

        venta.setCanal(canal);
        venta.setProducto(producto);
        venta.setFechaVenta(fechaVenta);
        venta.setPrecioVenta(precioVenta.setScale(2, RoundingMode.HALF_UP));
        venta.setCantidad(cantidad);
        venta.setDescuentoCampana(BigDecimal.ZERO);
        venta.setReferenciaExterna(orderId);
        venta.setNumeroOrden(orderId);

        Venta guardada = ventaRepository.save(venta);

        costoVentaRepository.deleteAll(costoVentaRepository.findByVentaId(guardada.getId()));
        if (comision != null && comision.signum() > 0) {
            costoVentaRepository.save(CostoVenta.builder()
                    .venta(guardada)
                    .tipo("COMISION")
                    .monto(comision.setScale(2, RoundingMode.HALF_UP))
                    .fuente("MERCADOLIBRE_IMPORT")
                    .build());
        }
        if (envio != null && envio.signum() > 0) {
            costoVentaRepository.save(CostoVenta.builder()
                    .venta(guardada)
                    .tipo("LOGISTICO")
                    .monto(envio.setScale(2, RoundingMode.HALF_UP))
                    .fuente("MERCADOLIBRE_IMPORT")
                    .build());
        }

        rentabilidadService.calcular(guardada);
        return ventaExistente != null ? ResultadoFila.ACTUALIZADO : ResultadoFila.CREADO;
    }

    private void actualizarCostoBaseProducto(String sku, BigDecimal costoMercadoLibre) {
        if (costoMercadoLibre == null || costoMercadoLibre.signum() <= 0) {
            return;
        }

        Optional<Producto> productoOpt = productoRepository.findBySku(sku);
        if (productoOpt.isEmpty()) {
            return;
        }

        Producto producto = productoOpt.get();
        if (producto.getCostoBase() == null || producto.getCostoBase().compareTo(costoMercadoLibre) != 0) {
            producto.setCostoBase(costoMercadoLibre);
            productoRepository.save(producto);
        }
    }

    private MercadoLibreCostoDto toDto(MercadoLibreCosto costo) {
        return MercadoLibreCostoDto.builder()
                .id(costo.getId())
                .sku(costo.getSku())
                .costoProm(costo.getCostoProm())
                .ultimoCosto(costo.getUltimoCosto())
                .costoMercadoLibre(costo.getCostoMercadoLibre())
                .fuenteArchivo(costo.getFuenteArchivo())
                .updatedAt(costo.getUpdatedAt())
                .build();
    }

    private Map<String, Integer> mapearColumnas(List<String> headers) {
        Map<String, Integer> columnas = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalizada = normalizar(headers.get(i));
            if (normalizada.equals("sku")) {
                columnas.putIfAbsent("sku", i);
            }
            if (normalizada.equals("costoprom") || normalizada.equals("costopromedio") || normalizada.equals("costo_prom")) {
                columnas.putIfAbsent("costo_prom", i);
            }
            if (normalizada.equals("ultimocosto") || normalizada.equals("ultimo_costo")) {
                columnas.putIfAbsent("ultimo_costo", i);
            }
            if (normalizada.equals("costomercadolibre") || normalizada.equals("costo_mercadolibre") || normalizada.equals("costo")) {
                columnas.putIfAbsent("costo_mercadolibre", i);
            }
        }

        if (!columnas.containsKey("sku")) {
            throw new IllegalArgumentException("No se encontró la columna SKU");
        }
        if (!columnas.containsKey("costo_mercadolibre")
                && !columnas.containsKey("costo_prom")
                && !columnas.containsKey("ultimo_costo")) {
            throw new IllegalArgumentException("El CSV debe incluir costo_mercadolibre o al menos costoProm/ultimoCosto");
        }

        return columnas;
    }

    private Map<String, Integer> mapearColumnasVentas(List<String> headers) {
        Map<String, Integer> columnas = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalizada = normalizar(headers.get(i));
            if (normalizada.equals("orderid") || normalizada.equals("order_id") || normalizada.equals("idorden") || normalizada.equals("numeroorden") || normalizada.equals("numero_orden")) {
                columnas.putIfAbsent("orderid", i);
            }
            if (normalizada.equals("fecha") || normalizada.equals("fecha_venta") || normalizada.equals("date")) {
                columnas.putIfAbsent("fecha", i);
            }
            if (normalizada.equals("sku") || normalizada.equals("seller_sku") || normalizada.equals("skuseller") || normalizada.equals("sku_producto")) {
                columnas.putIfAbsent("sku", i);
            }
            if (normalizada.equals("cantidad") || normalizada.equals("qty") || normalizada.equals("quantity")) {
                columnas.putIfAbsent("cantidad", i);
            }
            if (normalizada.equals("precioventa") || normalizada.equals("precio_venta") || normalizada.equals("precio") || normalizada.equals("price")) {
                columnas.putIfAbsent("precioventa", i);
            }
            if (normalizada.equals("comision") || normalizada.equals("comisión") || normalizada.equals("commission")) {
                columnas.putIfAbsent("comision", i);
            }
            if (normalizada.equals("envio") || normalizada.equals("shipping") || normalizada.equals("logistica") || normalizada.equals("shipping_cost")) {
                columnas.putIfAbsent("envio", i);
            }
            if (normalizada.equals("categoria") || normalizada.equals("category")) {
                columnas.putIfAbsent("categoria", i);
            }
            if (normalizada.equals("producto") || normalizada.equals("product") || normalizada.equals("nombreproducto") || normalizada.equals("nombre_producto")) {
                columnas.putIfAbsent("producto", i);
            }
        }

        if (!columnas.containsKey("orderid")) {
            throw new IllegalArgumentException("No se encontró la columna orderId");
        }
        if (!columnas.containsKey("fecha")) {
            throw new IllegalArgumentException("No se encontró la columna fecha");
        }
        if (!columnas.containsKey("sku")) {
            throw new IllegalArgumentException("No se encontró la columna SKU");
        }
        if (!columnas.containsKey("cantidad")) {
            throw new IllegalArgumentException("No se encontró la columna cantidad");
        }
        if (!columnas.containsKey("precioventa")) {
            throw new IllegalArgumentException("No se encontró la columna precioVenta");
        }

        return columnas;
    }

    private String valor(List<String> valores, Map<String, Integer> columnas, String nombre) {
        Integer idx = columnas.get(nombre);
        if (idx == null || idx >= valores.size()) {
            return "";
        }
        return valores.get(idx).replace("\"", "").trim();
    }

    private BigDecimal parsearNumero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor
                .replace("$", "")
                .replace("CLP", "")
                .replace(" ", "")
                .trim();

        if (limpio.isBlank() || limpio.equals("-")) {
            return null;
        }

        if (limpio.matches(".*,\\d{1,2}$")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (limpio.contains(".") && !limpio.contains(",")) {
            limpio = limpio.replace(".", "");
        } else {
            limpio = limpio.replace(",", "");
        }

        return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer parsearEntero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    return LocalDate.parse(limpio, formatter);
                }
                if (limpio.length() >= 10 && limpio.contains(" ")) {
                    return LocalDate.parse(limpio.substring(0, 10), formatter);
                }
                return LocalDate.parse(limpio, formatter);
            } catch (DateTimeParseException ignored) {
                // Intenta el siguiente formato.
            }
        }
        return null;
    }

    private char detectarSeparador(String linea) {
        int comas = linea.length() - linea.replace(",", "").length();
        int puntosComa = linea.length() - linea.replace(";", "").length();
        return puntosComa > comas ? ';' : ',';
    }

    private List<String> parsearLinea(String linea, char separador) {
        List<String> resultado = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                entreComillas = !entreComillas;
            } else if (c == separador && !entreComillas) {
                resultado.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        resultado.add(actual.toString().trim());
        return resultado;
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }

    private String formatearNumero(BigDecimal numero) {
        return numero == null ? "" : numero.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escaparCsv(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private enum ResultadoFila {
        CREADO, ACTUALIZADO, OMITIDO
    }
}
