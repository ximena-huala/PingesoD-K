package cl.dk.rentabilidad.integration.bsale;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/**
 * Lee exportaciones de Bsale en formato Excel (.xlsx) o CSV.
 * Detecta columnas por nombre flexible (sin importar mayúsculas ni tildes).
 */
@Component
public class BsaleSpreadsheetReader {

    public enum TipoExportacion {
        PRODUCTOS,
        STOCK
    }

    public record FilaImportacion(Map<String, String> valores) {
        public String get(String columna) {
            return valores.getOrDefault(columna, "").trim();
        }
    }

    public List<FilaImportacion> leer(MultipartFile archivo, TipoExportacion tipo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BsaleImportException("El archivo está vacío");
        }
        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase()
                : "";

        try {
            if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
                return leerCsv(archivo, tipo);
            }
            if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
                return leerExcel(archivo, tipo);
            }
            // Intentar Excel primero; si falla, CSV
            try {
                return leerExcel(archivo, tipo);
            } catch (Exception e) {
                return leerCsv(archivo, tipo);
            }
        } catch (BsaleImportException e) {
            throw e;
        } catch (Exception e) {
            throw new BsaleImportException("No se pudo leer el archivo: " + e.getMessage(), e);
        }
    }

    private List<FilaImportacion> leerExcel(MultipartFile archivo, TipoExportacion tipo) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BsaleImportException("El Excel no tiene hojas");
            }

            int headerRowIndex = encontrarFilaEncabezado(sheet, tipo);
            Row headerRow = sheet.getRow(headerRowIndex);
            Map<String, Integer> columnas = mapearColumnas(headerRow, tipo);

            List<FilaImportacion> filas = new ArrayList<>();
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || filaVacia(row)) {
                    continue;
                }
                Map<String, String> valores = new LinkedHashMap<>();
                columnas.forEach((nombre, idx) ->
                        valores.put(nombre, leerCelda(row.getCell(idx))));
                if (tieneDatosUtiles(valores, tipo)) {
                    filas.add(new FilaImportacion(valores));
                }
            }
            validarFilas(filas, tipo);
            return filas;
        }
    }

    private List<FilaImportacion> leerCsv(MultipartFile archivo, TipoExportacion tipo) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String primeraLinea = reader.readLine();
            if (primeraLinea == null) {
                throw new BsaleImportException("El CSV está vacío");
            }

            char separador = detectarSeparador(primeraLinea);
            List<String> headersRaw = parsearLinea(primeraLinea, separador);
            Map<String, Integer> columnas = mapearColumnasTexto(headersRaw, tipo);

            List<FilaImportacion> filas = new ArrayList<>();
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                List<String> celdas = parsearLinea(linea, separador);
                Map<String, String> valores = new LinkedHashMap<>();
                columnas.forEach((nombre, idx) ->
                        valores.put(nombre, idx < celdas.size() ? celdas.get(idx).trim() : ""));
                if (tieneDatosUtiles(valores, tipo)) {
                    filas.add(new FilaImportacion(valores));
                }
            }
            validarFilas(filas, tipo);
            return filas;
        }
    }

    private int encontrarFilaEncabezado(Sheet sheet, TipoExportacion tipo) {
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 10); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            try {
                mapearColumnas(row, tipo);
                return i;
            } catch (BsaleImportException ignored) {
                // seguir buscando
            }
        }
        throw new BsaleImportException("No se encontró fila de encabezados reconocible en el archivo");
    }

    private Map<String, Integer> mapearColumnas(Row headerRow, TipoExportacion tipo) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            headers.add(leerCelda(headerRow.getCell(i)));
        }
        return mapearColumnasTexto(headers, tipo);
    }

    private Map<String, Integer> mapearColumnasTexto(List<String> headers, TipoExportacion tipo) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        Set<String> requeridas = columnasRequeridas(tipo);
        Set<String> opcionales = columnasOpcionales(tipo);
        Map<String, List<String>> alias = aliasPorColumna(tipo);

        for (Map.Entry<String, List<String>> entry : alias.entrySet()) {
            String columnaLogica = entry.getKey();
            for (int i = 0; i < headers.size(); i++) {
                String header = normalizar(headers.get(i));
                if (entry.getValue().stream().anyMatch(a -> header.contains(normalizar(a)))) {
                    resultado.putIfAbsent(columnaLogica, i);
                    break;
                }
            }
        }

        List<String> faltantes = requeridas.stream()
                .filter(c -> !resultado.containsKey(c))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new BsaleImportException(
                    "Columnas obligatorias no encontradas: " + String.join(", ", faltantes)
                            + ". Encabezados detectados: " + headers);
        }

        opcionales.forEach(c -> alias.getOrDefault(c, List.of()).forEach(a -> {
            for (int i = 0; i < headers.size(); i++) {
                if (normalizar(headers.get(i)).contains(normalizar(a))) {
                    resultado.putIfAbsent(c, i);
                    break;
                }
            }
        }));

        return resultado;
    }

    private Set<String> columnasRequeridas(TipoExportacion tipo) {
        return switch (tipo) {
            case PRODUCTOS -> Set.of("nombre");
            case STOCK -> Set.of("sku");
        };
    }

    private Set<String> columnasOpcionales(TipoExportacion tipo) {
        return switch (tipo) {
            case PRODUCTOS -> Set.of("sku", "estado", "marca", "tipo", "categoria");
            case STOCK -> Set.of("nombre", "variante", "marca", "tipo", "stock", "costo");
        };
    }

    private Map<String, List<String>> aliasPorColumna(TipoExportacion tipo) {
        Map<String, List<String>> mapa = new LinkedHashMap<>();
        if (tipo == TipoExportacion.PRODUCTOS) {
            mapa.put("sku", List.of("sku", "codigo", "código", "codigo de barras", "barcode"));
            mapa.put("nombre", List.of("producto", "nombre", "descripcion", "descripción"));
            mapa.put("estado", List.of("estado"));
            mapa.put("marca", List.of("marca"));
            mapa.put("tipo", List.of("tipo de producto", "tipo producto", "tipo"));
            mapa.put("categoria", List.of("categoria", "categoría", "familia"));
        } else {
            mapa.put("sku", List.of("sku"));
            mapa.put("nombre", List.of("producto", "nombre", "descripcion", "descripción"));
            mapa.put("variante", List.of("variante"));
            mapa.put("marca", List.of("marca"));
            mapa.put("tipo", List.of("tipo de producto", "tipo producto", "tipo"));
            mapa.put("stock", List.of("stock", "cantidad disponible", "disponible", "total"));
            mapa.put("costo", List.of(
                    "costo neto prom. unitario",
                    "costo neto prom unitario",
                    "ultimo costo",
                    "último costo",
                    "costo unitario promedio",
                    "costo promedio",
                    "costo unitario",
                    "costo"));
        }
        return mapa;
    }

    private boolean tieneDatosUtiles(Map<String, String> valores, TipoExportacion tipo) {
        if (!valores.getOrDefault("nombre", "").isBlank()) {
            return true;
        }
        return !valores.getOrDefault("sku", "").isBlank();
    }

    private void validarFilas(List<FilaImportacion> filas, TipoExportacion tipo) {
        if (filas.isEmpty()) {
            throw new BsaleImportException("El archivo no contiene filas de datos válidas");
        }
    }

    private boolean filaVacia(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!leerCelda(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String leerCelda(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private char detectarSeparador(String linea) {
        int comas = linea.length() - linea.replace(",", "").length();
        int puntos = linea.length() - linea.replace(";", "").length();
        return puntos > comas ? ';' : ',';
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

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).trim();
    }
}
