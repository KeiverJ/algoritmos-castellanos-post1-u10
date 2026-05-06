package com.university.pbt;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Anillo de hashing consistente con nodos virtuales.
 *
 * <p>Cada nodo físico se representa mediante {@code virtualNodes} puntos en el
 * anillo, distribuidos mediante la función {@link #hash(String)}.  Esto reduce
 * la varianza en la distribución de claves entre nodos.
 *
 * <p>Complejidad:
 * <ul>
 *   <li>addNode / removeNode: O(v · log(n·v)) — v = virtualNodes, n = nodos físicos</li>
 *   <li>getNode: O(log(n·v)) — búsqueda de techo en TreeMap</li>
 * </ul>
 */
public class ConsistentHashRing {

    /** Anillo ordenado: hash → nombre del nodo físico. */
    private final TreeMap<Integer, String> ring = new TreeMap<>();

    /** Número de nodos virtuales por nodo físico. */
    private final int virtualNodes;

    /**
     * Crea un anillo con los nodos iniciales dados.
     *
     * @param nodes        lista de nombres de nodos físicos (no vacía)
     * @param virtualNodes cantidad de réplicas virtuales por nodo (≥ 1)
     * @throws IllegalArgumentException si {@code virtualNodes} &lt; 1
     */
    public ConsistentHashRing(List<String> nodes, int virtualNodes) {
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("virtualNodes debe ser ≥ 1");
        }
        this.virtualNodes = virtualNodes;
        nodes.forEach(this::addNode);
    }

    /**
     * Agrega un nodo físico al anillo creando sus réplicas virtuales.
     *
     * @param node nombre del nodo a agregar
     */
    public void addNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            int h = hash(node + "#" + i);
            ring.put(h, node);
        }
    }

    /**
     * Elimina un nodo físico y todas sus réplicas virtuales del anillo.
     *
     * @param node nombre del nodo a eliminar
     */
    public void removeNode(String node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.remove(hash(node + "#" + i));
        }
    }

    /**
     * Determina el nodo responsable de la clave dada.
     *
     * <p>La clave se mapea al primer nodo cuyo hash sea ≥ al hash de la clave
     * (wrap-around al primer nodo si no existe techo).
     *
     * @param key clave a enrutar (no nula, no vacía)
     * @return nombre del nodo responsable
     * @throws IllegalStateException si el anillo está vacío
     */
    public String getNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("El anillo está vacío; agregue al menos un nodo.");
        }
        int h = hash(key);
        Map.Entry<Integer, String> entry = ring.ceilingEntry(h);
        // Wrap-around: si no hay nodo con hash ≥ h, usamos el primer nodo del anillo
        return (entry != null ? entry : ring.firstEntry()).getValue();
    }

    /**
     * Retorna el conjunto de nodos físicos actualmente en el anillo.
     *
     * @return conjunto inmutable con los nombres de los nodos
     */
    public Set<String> nodes() {
        return new HashSet<>(ring.values());
    }

    /**
     * Función de hash FNV-1a (Fowler–Noll–Vo) de 32 bits.
     *
     * <p>FNV-1a ofrece mejor avalancha bit a bit que {@link String#hashCode()}
     * para cadenas cortas (3–10 chars), reduciendo la varianza en la distribución
     * de nodos virtuales sobre el anillo y haciendo que la propiedad de migración
     * parcial sea estadísticamente confiable incluso con 2 nodos físicos.
     *
     * @param key cadena a hashear
     * @return valor entero no negativo en [0, {@link Integer#MAX_VALUE}]
     */
    private int hash(String key) {
        int hash = 0x811c9dc5;          // FNV offset basis (32-bit)
        final int prime = 0x01000193;   // FNV prime (32-bit)
        for (byte b : key.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            hash ^= (b & 0xFF);
            hash *= prime;
        }
        return hash & Integer.MAX_VALUE; // garantizar valor no negativo
    }
}
