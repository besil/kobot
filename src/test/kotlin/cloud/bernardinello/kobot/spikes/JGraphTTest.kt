package cloud.bernardinello.kobot.spikes

import cloud.bernardinello.kobot.utils.LogUtils
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JGraphTTest : StringSpec() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(JGraphTTest::class.java)
    }

    class Data(val key: String, val value: Int) {
        override fun toString(): String = "$key"
    }

    class Node(val data: Data) {
        override fun toString(): String = data.toString()
        override fun hashCode(): Int = data.toString().hashCode()
        override fun equals(other: Any?): Boolean = (other is Node) && (this.toString() == other.toString())
    }

    class Edge(val x: Int) : DefaultEdge() {
        override fun toString(): String = x.toString()
    }

    init {
        LogUtils.setLogLevel(default = "warn")

        "A simple graph should be" {
            val g: Graph<Node, Edge> = SimpleDirectedWeightedGraph(
                Edge::class.java
            )

            val nodeMap: Map<String, Node> = mapOf(
                "k0" to Node(
                    Data(
                        "k0",
                        value = 0
                    )
                ),
                "k1" to Node(
                    Data(
                        "k1",
                        value = 1
                    )
                ),
                "k2" to Node(
                    Data(
                        "k2",
                        value = 2
                    )
                ),
                "k3" to Node(
                    Data(
                        "k3",
                        value = 3
                    )
                )
            )

            g.addVertex(nodeMap["k0"])
            g.addVertex(nodeMap["k1"])
            g.addVertex(nodeMap["k2"])
            g.addVertex(nodeMap["k3"])

            g.addEdge(
                nodeMap["k0"], nodeMap["k1"],
                Edge(1)
            )

            log.debug("Graph is: {}", g)

            val connectivity: ConnectivityInspector<Node, Edge> = ConnectivityInspector(g)
            log.debug("Connectivity: {}", connectivity.connectedSets())
            connectivity.isConnected shouldBe false

            log.debug("Connected sets: {}", connectivity.connectedSets())
            log.debug("Connected sets size: {}", connectivity.connectedSets().size)
            connectivity.connectedSets().size shouldBe 3
            connectivity.pathExists(nodeMap["k0"], nodeMap["k1"]) shouldBe true

            log.debug("Going strong...")

            val scAlg: StrongConnectivityAlgorithm<Node, Edge> = KosarajuStrongConnectivityInspector(g)
            scAlg.isStronglyConnected shouldBe false
            scAlg.stronglyConnectedComponents.size shouldBe 4
        }
    }
}