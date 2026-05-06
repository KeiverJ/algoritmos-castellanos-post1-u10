package com.university.pbt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lista ordenada que mantiene sus elementos en orden no decreciente.
 *
 * <p>Invariante de clase: en todo momento {@code isSorted()} retorna {@code true}.
 * La inserción se realiza mediante búsqueda binaria para garantizar O(log n)
 * búsqueda y O(n) inserción en el peor caso.
 *
 * @param <T> tipo de elemento, debe ser {@link Comparable}
 */
public class SortedList<T extends Comparable<T>> {

    /** Almacenamiento interno; siempre mantenido en orden no decreciente. */
    private final List<T> data = new ArrayList<>();

    /**
     * Agrega un elemento manteniendo el invariante de orden.
     *
     * @param element elemento a insertar (puede ser {@code null} si T lo admite,
     *                aunque el comportamiento no está definido en ese caso)
     */
    public void add(T element) {
        int pos = Collections.binarySearch(data, element);
        // binarySearch retorna índice negativo cuando no encuentra el elemento:
        // pos = -(insertion_point) - 1  →  insertion_point = -(pos + 1)
        if (pos < 0) {
            pos = -(pos + 1);
        }
        data.add(pos, element);
    }

    /**
     * Agrega todos los elementos de la colección dada, manteniendo el invariante.
     *
     * @param elements colección de elementos a insertar
     */
    public void addAll(List<T> elements) {
        elements.forEach(this::add);
    }

    /**
     * Retorna una vista no modificable de los datos internos.
     *
     * @return lista inmutable con los elementos en orden no decreciente
     */
    public List<T> toList() {
        return Collections.unmodifiableList(data);
    }

    /** @return número de elementos en la lista */
    public int size() {
        return data.size();
    }

    /** @return {@code true} si la lista no contiene elementos */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Verifica el invariante de orden de la lista.
     *
     * @return {@code true} si cada elemento es ≤ al siguiente; {@code true}
     *         también para listas vacías o de un elemento
     */
    public boolean isSorted() {
        for (int i = 0; i < data.size() - 1; i++) {
            if (data.get(i).compareTo(data.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }
}
