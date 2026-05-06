package com.university.pbt;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Propiedad round-trip para {@link SortedList} con serialización JSON.
 *
 * <p>Una propiedad round-trip verifica que para cualquier valor {@code x},
 * la composición {@code decode(encode(x)) == x} se cumple.  Aquí:
 * <ul>
 *   <li>{@code encode}: serializar {@link SortedList#toList()} a JSON con Jackson.</li>
 *   <li>{@code decode}: deserializar el JSON a una {@code List<Integer>}.</li>
 * </ul>
 *
 * <p>Esta propiedad garantiza que ningún elemento se pierde, corrompe o reordena
 * durante el ciclo de serialización/deserialización.
 */
class RoundTripProperties {

    /** Mapper de Jackson reutilizable (thread-safe después de configuración). */
    private final ObjectMapper mapper = new ObjectMapper();

    // ── Propiedad: round-trip JSON ────────────────────────────────────────────
    /**
     * Para cualquier lista de enteros, serializar el contenido de
     * {@link SortedList} a JSON y luego deserializarlo debe producir
     * exactamente la misma lista ordenada.
     *
     * <p>Detectaría bugs como:
     * <ul>
     *   <li>Pérdida de elementos durante la serialización.</li>
     *   <li>Cambio de tipo numérico (e.g., Integer → Long) en la deserialización.</li>
     *   <li>Reordenamiento inadvertido del array JSON.</li>
     * </ul>
     *
     * @param elements lista generada aleatoriamente por jqwik
     * @throws Exception si Jackson falla (el test fallará con evidencia clara)
     */
    @Property
    void jsonRoundTrip(@ForAll List<Integer> elements) throws Exception {

        // 1. Construir la lista ordenada con los elementos del input
        SortedList<Integer> original = new SortedList<>();
        original.addAll(elements);

        // 2. Serializar a JSON
        String json = mapper.writeValueAsString(original.toList());

        // 3. Deserializar desde JSON
        List<Integer> deserialized = mapper.readValue(
                json,
                mapper.getTypeFactory().constructCollectionType(List.class, Integer.class)
        );

        // 4. Round-trip: decode(encode(x)) == x
        assertThat(deserialized)
                .as("Round-trip JSON debe preservar todos los elementos en el mismo orden.\n"
                        + "Input original: %s\nJSON: %s", elements, json)
                .isEqualTo(original.toList());
    }
}
