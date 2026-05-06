package com.university.pbt;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Suite de propiedades algebraicas para {@link SortedList}.
 *
 * <p>Propiedades verificadas:
 * <ol>
 *   <li><b>Invariante de orden</b>: tras insertar cualquier colección, la lista
 *       siempre está ordenada de forma no decreciente.</li>
 *   <li><b>Preservación de elementos</b>: todos los elementos del input aparecen
 *       en la lista (sin pérdidas ni duplicaciones).</li>
 *   <li><b>Tamaño consistente</b>: el tamaño de la lista coincide con el del input.</li>
 *   <li><b>Idempotencia de inserción ordenada</b>: insertar una lista ya ordenada
 *       produce el mismo resultado que insertar la lista original.</li>
 * </ol>
 *
 * <p>Cada propiedad se ejecuta con 1 000 casos generados aleatoriamente (tries).
 */
class SortedListProperties {

    // ── Propiedad 1: invariante de orden ─────────────────────────────────────
    /**
     * Para cualquier lista de enteros en [-1000, 1000], después de agregarlos
     * todos a {@link SortedList}, la lista debe estar ordenada de forma no
     * decreciente.
     *
     * <p>Captura la garantía fundamental de la estructura: el invariante de orden
     * se mantiene independientemente del orden de inserción.
     */
    @Property
    void alwaysSorted(
            @ForAll List<@IntRange(min = -1000, max = 1000) Integer> elements) {

        SortedList<Integer> list = new SortedList<>();
        list.addAll(elements);

        assertThat(list.isSorted())
                .as("La lista debe estar ordenada tras insertar: %s", elements)
                .isTrue();
    }

    // ── Propiedad 2: preserva todos los elementos ─────────────────────────────
    /**
     * Para cualquier lista de enteros, {@link SortedList} debe contener
     * exactamente los mismos elementos (en cualquier orden), sin pérdidas ni
     * duplicados espurios.
     *
     * <p>Detectaría bugs como: inserciones que sobrescriben elementos, búsquedas
     * binarias con off-by-one que descartan elementos, o adición condicional
     * incorrecta.
     */
    @Property
    void preservesAllElements(@ForAll List<Integer> elements) {

        SortedList<Integer> list = new SortedList<>();
        list.addAll(elements);

        assertThat(list.toList())
                .as("La lista debe contener exactamente los elementos del input")
                .containsExactlyInAnyOrderElementsOf(elements);
    }

    // ── Propiedad 3: tamaño igual al input ────────────────────────────────────
    /**
     * El tamaño de {@link SortedList} debe coincidir con el número de elementos
     * insertados.
     *
     * <p>Complementa la propiedad 2: si se insertan duplicados, también deben
     * contabilizarse en el tamaño.
     */
    @Property
    void sizeMatchesInput(@ForAll List<Integer> elements) {

        SortedList<Integer> list = new SortedList<>();
        list.addAll(elements);

        assertThat(list.size())
                .as("El tamaño debe ser igual al número de elementos insertados")
                .isEqualTo(elements.size());
    }

    // ── Propiedad 4: idempotencia de inserción ordenada ───────────────────────
    /**
     * Insertar una lista ya ordenada (la salida de una {@link SortedList}) en
     * una nueva instancia debe producir exactamente el mismo resultado.
     *
     * <p>Verifica que la propiedad de idempotencia se cumple: ordenar una lista
     * ya ordenada no la altera.  Si {@code addAll(sorted)} modificase el orden,
     * esta propiedad lo detectaría.
     */
    @Property
    void addingAlreadySortedGivesSameResult(@ForAll List<Integer> elements) {

        SortedList<Integer> list1 = new SortedList<>();
        list1.addAll(elements);

        SortedList<Integer> list2 = new SortedList<>();
        list2.addAll(list1.toList()); // insertar la salida ya ordenada

        assertThat(list2.toList())
                .as("Insertar una lista ya ordenada debe producir el mismo resultado")
                .isEqualTo(list1.toList());
    }
}
