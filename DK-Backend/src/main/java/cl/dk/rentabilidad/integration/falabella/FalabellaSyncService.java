package cl.dk.rentabilidad.integration.falabella;

import cl.dk.rentabilidad.entity.CanalVenta;
import cl.dk.rentabilidad.entity.Producto;
import cl.dk.rentabilidad.entity.Venta;
import cl.dk.rentabilidad.integration.falabella.dto.FalabellaSyncResult;
import cl.dk.rentabilidad.repository.CanalVentaRepository;
import cl.dk.rentabilidad.repository.ProductoRepository;
import cl.dk.rentabilidad.repository.VentaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trae las ventas de Falabella y las deja en la tabla {@code venta}.
 *
 * Por cada orden creada desde la fecha indicada pide sus items y, para los que
 * están "delivered" (el estado relevante va por item, no por orden), busca el
 * producto por SKU y guarda una venta. El cruce es directo: el SKU que reporta
 * la orden es el mismo SellerSku con que se sembró {@code producto}.
 *
 * La sincronización es idempotente: usa el OrderItemId como referencia externa,
 * así que volver a correrla actualiza en vez de duplicar. Los items cuyo SKU no
 * existe en {@code producto} no se pierden silenciosamente: se cuentan y se
 * devuelven en el resultado para revisarlos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FalabellaSyncService {

    private static final String CANAL = "Falabella";

    private final FalabellaClient client;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final CanalVentaRepository canalVentaRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public FalabellaSyncResult sincronizarVentas(LocalDate desde) {
        CanalVenta canal = canalVentaRepository.findByNombre(CANAL)
            .orElseThrow(() -> new IllegalStateException("No existe el canal '" + CANAL + "' en la base"));

        // GetOrders con solo CreatedAfter devuelve una ventana acotada (~2 meses),
        // no todo lo posterior a la fecha. Para traer todos los meses recorremos
        // ventanas mensuales [inicio, fin) con CreatedAfter + CreatedBefore, y dentro
        // de cada ventana paginamos por offset (diciembre, por ejemplo, supera 100).
        List<JsonNode> ordenes = new ArrayList<>();
        int pageSize = 100;
        LocalDate hoy = LocalDate.now();
        for (LocalDate ini = desde; ini.isBefore(hoy.plusDays(1)); ini = ini.plusMonths(1)) {
            LocalDate fin = ini.plusMonths(1);
            int offset = 0;
            while (true) {
                Map<String, String> params = new HashMap<>();
                params.put("CreatedAfter", ini + "T00:00:00-04:00");
                params.put("CreatedBefore", fin + "T00:00:00-04:00");
                params.put("Limit", String.valueOf(pageSize));
                params.put("Offset", String.valueOf(offset));
                List<JsonNode> pagina = nodos(client.call("GetOrders", params), "Orders", "Order");
                if (pagina.isEmpty()) break;
                ordenes.addAll(pagina);
                if (pagina.size() < pageSize) break;
                offset += pageSize;
                pausa();
            }
        }
        log.info("Sincronización Falabella: {} órdenes desde {}", ordenes.size(), desde);

        int itemsTotal = 0, noEntregados = 0, creadas = 0, actualizadas = 0, sinProducto = 0;
        List<String> skusSinProducto = new ArrayList<>();

        for (JsonNode orden : ordenes) {
            String orderId = orden.path("OrderId").asText();
            String numeroOrden = orden.path("OrderNumber").asText();
            LocalDate fecha = fechaDe(orden.path("CreatedAt").asText());

            for (JsonNode item : nodos(client.getOrderItems(orderId), "OrderItems", "OrderItem")) {
                itemsTotal++;
                if (!"delivered".equalsIgnoreCase(item.path("Status").asText())) {
                    noEntregados++;
                    continue;
                }

                String sku = item.path("Sku").asText().trim();
                Producto producto = productoRepository.findBySku(sku).orElse(null);
                if (producto == null) {
                    sinProducto++;
                    if (skusSinProducto.size() < 100) skusSinProducto.add(sku);
                    continue;
                }

                String referencia = item.path("OrderItemId").asText();
                Venta venta = ventaRepository.findByReferenciaExterna(referencia).orElse(null);
                boolean nueva = venta == null;
                if (nueva) venta = new Venta();

                venta.setCanal(canal);
                venta.setProducto(producto);
                venta.setFechaVenta(fecha);
                venta.setPrecioVenta(monto(item.path("PaidPrice").asText()));
                venta.setCantidad(1);
                venta.setDescuentoCampana(monto(item.path("VoucherAmount").asText()));
                venta.setReferenciaExterna(referencia);
                venta.setNumeroOrden(numeroOrden);
                ventaRepository.save(venta);

                if (nueva) creadas++; else actualizadas++;
            }

            pausa();
        }

        if (sinProducto > 0) {
            log.warn("Falabella: {} items sin producto en la base. SKU (muestra): {}",
                sinProducto, skusSinProducto.stream().limit(15).toList());
        }
        return new FalabellaSyncResult(ordenes.size(), itemsTotal, noEntregados,
            creadas, actualizadas, sinProducto, skusSinProducto, LocalDateTime.now());
    }

    /** Devuelve la lista bajo wrapper.element, tolerando que Falabella mande un objeto o un arreglo. */
    private List<JsonNode> nodos(String body, String wrapper, String element) {
        try {
            JsonNode node = mapper.readTree(body).path(wrapper).path(element);
            List<JsonNode> out = new ArrayList<>();
            if (node.isArray()) node.forEach(out::add);
            else if (node.isObject()) out.add(node);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo parsear la respuesta de Falabella", e);
        }
    }

    /** CreatedAt llega como "2026-06-16 23:35:33"; nos quedamos con la fecha. */
    private LocalDate fechaDe(String createdAt) {
        try {
            return LocalDate.parse(createdAt.substring(0, 10));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private BigDecimal monto(String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** Pausa conservadora entre llamadas para no estresar la API (sin límite documentado). */
    private void pausa() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
