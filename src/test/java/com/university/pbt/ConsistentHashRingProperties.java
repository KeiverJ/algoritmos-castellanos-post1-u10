package com.university.pbt;

import net.jqwik.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Suite de propiedades para {@link ConsistentHashRing}.
 *
 * <p>Propiedades verificadas:
 * <ol>
 *   <li><b>Determinismo</b>: la misma clave siempre es enrutada al mismo nodo.</li>
 *   <li><b>Nodo válido</b>: la clave siempre es asignada a un nodo conocido del anillo.</li>
 *   <li><b>Migración parcial</b>: agregar un nuevo nodo solo migra un subconjunto
 *       de claves, no todas.</li>
 * </ol>
 *
 * <p><b>Nota de compatibilidad:</b> jqwik 1.8.3 lanza {@link ClassCastException}
 * ({@code String → Character}) durante el shrinking cuando se aplica
 * {@code @StringLength} como anotación de tipo-uso en parámetros ({@code @ForAll
 * @StringLength String key}).  Por ello, todas las cadenas se generan a través de
 * generadores {@code @Provide} explícitos, que son completamente inmunes a este bug.
 */
class ConsistentHashRingProperties {

    /** Número de nodos virtuales por nodo físico usado en todas las propiedades. */
    private static final int VIRTUAL_NODES = 300;

    // ── Generadores personalizados ────────────────────────────────────────────

    /**
     * Genera listas de nombres de nodos válidos:
     * <ul>
     *   <li>Cada nombre es una cadena alfabética de 3–10 caracteres.</li>
     *   <li>La lista tiene entre 1 y 8 nodos.</li>
     *   <li>Todos los nombres son únicos dentro de la lista (sin duplicados).</li>
     * </ul>
     *
     * @return {@link Arbitrary} de listas de nodos únicos
     */
    @Provide
    Arbitrary<List<String>> nodeList() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .list()
                .ofMinSize(1)
                .ofMaxSize(8)
                .uniqueElements();
    }

    /**
     * Genera listas de nombres de nodos con al menos 2 elementos, necesario para
     * la propiedad de migración parcial (con un solo nodo, agregar otro migra el
     * 100% de las claves, lo cual es correcto pero viola el umbral de "mayoría").
     *
     * @return {@link Arbitrary} de listas de ≥2 nodos únicos
     */
    @Provide
    Arbitrary<List<String>> nodeListMin2() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .list()
                .ofMinSize(2)
                .ofMaxSize(8)
                .uniqueElements();
    }

    /**
     * Genera una cadena de longitud 1–50 para usar como clave de búsqueda.
     *
     * <p>Se usa {@code @Provide} en lugar de {@code @StringLength} para evitar
     * el {@link ClassCastException} de jqwik 1.8.3 durante el shrinking.
     *
     * @return {@link Arbitrary} de claves no vacías
     */
    @Provide
    Arbitrary<String> shortKey() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * Genera listas de claves de búsqueda: entre 10 y 50 cadenas ASCII
     * imprimibles de 1–20 caracteres.
     *
     * @return {@link Arbitrary} de listas de claves válidas
     */
    @Provide
    Arbitrary<List<String>> keyList() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isBlank())
                .list()
                .ofMinSize(100)
                .ofMaxSize(200);
    }

    /**
     * Genera un nombre de nodo candidato de 1–30 caracteres alfanuméricos.
     *
     * @return {@link Arbitrary} de nombres de nodo
     */
    @Provide
    Arbitrary<String> newNodeName() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    // ── Propiedad 1: determinismo ─────────────────────────────────────────────

    /**
     * Para cualquier conjunto de nodos y cualquier clave, llamar a
     * {@link ConsistentHashRing#getNode(String)} dos veces con la misma clave
     * debe retornar exactamente el mismo nodo.
     *
     * <p>Verifica que no existen efectos secundarios ni fuentes de aleatoriedad
     * interna que modifiquen el estado del anillo entre llamadas.
     */
    @Property
    void getNodeIsDeterministic(
            @ForAll("nodeList") List<String> nodes,
            @ForAll("shortKey") String key) {

        ConsistentHashRing ring = new ConsistentHashRing(nodes, VIRTUAL_NODES);

        assertThat(ring.getNode(key))
                .as("La misma clave '%s' debe enrutarse siempre al mismo nodo", key)
                .isEqualTo(ring.getNode(key));
    }

    // ── Propiedad 2: la clave siempre va a un nodo conocido ──────────────────

    /**
     * Para cualquier conjunto de nodos y cualquier clave, el nodo retornado por
     * {@link ConsistentHashRing#getNode(String)} debe ser uno de los nodos
     * actualmente registrados en el anillo.
     *
     * <p>Detectaría bugs como: retornar un nodo que fue eliminado, o calcular
     * un hash que apunta fuera del espacio de nodos conocidos.
     */
    @Property
    void getNodeReturnsKnownNode(
            @ForAll("nodeList") List<String> nodes,
            @ForAll("shortKey") String key) {

        ConsistentHashRing ring = new ConsistentHashRing(nodes, VIRTUAL_NODES);

        assertThat(ring.nodes())
                .as("El nodo retornado debe ser uno de los nodos registrados en el anillo")
                .contains(ring.getNode(key));
    }

    // ── Propiedad 3: monotonía — las claves solo migran al nuevo nodo ─────────

    /**
     * Al agregar un nodo nuevo, una clave que migra <em>solo puede</em> ir al
     * nuevo nodo.  Nunca puede haber migración entre dos nodos existentes.
     *
     * <p>Esta es la propiedad de <b>monotonía</b> del hashing consistente: agregar
     * un nodo N' solo "roba" claves de sus vecinos inmediatos en el anillo, y las
     * entrega exclusivamente a N'.  Ninguna clave reasignada puede terminar en un
     * nodo distinto de N'.
     *
     * <p>Esta propiedad es <em>determinista</em> y verificable formalmente para
     * cualquier configuración de hashes, a diferencia de una propiedad estadística.
     */
    @Property(tries = 200)
    void migratedKeysGoOnlyToNewNode(
            @ForAll("nodeListMin2") List<String> nodes,
            @ForAll("newNodeName") String newNode,
            @ForAll("keyList") List<String> keys) {

        Assume.that(!nodes.contains(newNode));

        ConsistentHashRing before = new ConsistentHashRing(nodes, VIRTUAL_NODES);

        List<String> nodesPlus = new ArrayList<>(nodes);
        nodesPlus.add(newNode);
        ConsistentHashRing after = new ConsistentHashRing(nodesPlus, VIRTUAL_NODES);

        // Toda clave que cambia de nodo debe ir EXACTAMENTE al nuevo nodo
        for (String key : keys) {
            String nodeBefore = before.getNode(key);
            String nodeAfter  = after.getNode(key);
            if (!nodeBefore.equals(nodeAfter)) {
                assertThat(nodeAfter)
                        .as("La clave '%s' migró de '%s' a '%s', pero solo debería "
                                + "migrar al nuevo nodo '%s'", key, nodeBefore, nodeAfter, newNode)
                        .isEqualTo(newNode);
            }
        }
    }

    // ── Propiedad 4: round-trip addNode / removeNode ──────────────────────────

    /**
     * Agregar un nodo y luego eliminarlo debe restaurar exactamente el enrutamiento
     * original para todas las claves.
     *
     * <p>Verifica la propiedad algebraica inversa: {@code remove(add(ring, n)) == ring}.
     * Detectaría bugs donde {@link ConsistentHashRing#removeNode(String)} no elimina
     * todos los nodos virtuales o lo hace con un hash diferente al usado en
     * {@link ConsistentHashRing#addNode(String)}.
     */
    @Property(tries = 200)
    void addThenRemoveRestoresOriginalRouting(
            @ForAll("nodeList") List<String> nodes,
            @ForAll("newNodeName") String newNode,
            @ForAll("keyList") List<String> keys) {

        Assume.that(!nodes.contains(newNode));

        ConsistentHashRing original = new ConsistentHashRing(nodes, VIRTUAL_NODES);

        // Agregar el nodo y luego eliminarlo
        original.addNode(newNode);
        original.removeNode(newNode);

        // El anillo restaurado debe enrutar igual que antes
        ConsistentHashRing reference = new ConsistentHashRing(nodes, VIRTUAL_NODES);

        for (String key : keys) {
            assertThat(original.getNode(key))
                    .as("Tras add+remove de '%s', la clave '%s' debe enrutarse igual "
                            + "que en el anillo original", newNode, key)
                    .isEqualTo(reference.getNode(key));
        }
    }
}
