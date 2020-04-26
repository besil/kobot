package cloud.bernardinello.kobot.conversation

import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.jgrapht.graph.EdgeReversedGraph
import org.jgrapht.traverse.BreadthFirstIterator
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class BotConfigException(message: String) : Exception(message)

class BotConfig(val states: List<BotState>, val relationships: List<BotStateRelationship>) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BotConfig::class.java)
    }

    fun statesUntilWait(state: BotState, onInput: List<String>): List<BotState> {
        log.trace("Starting search from {} node with onInput: {}", state, onInput)
        val node: BotNode = graph.vertexSet().find { it.state == state }
            ?: throw BotConfigException("No state with id '${state.id}' found")

        val root: BotNode = when (state) {
            is WaitForInputState -> {
                log.trace("Handling a wait-forinput state")
                val outEdges: Set<BotEdge> = graph.outgoingEdgesOf(node)

                val edge: BotEdge = if (state.expectedValues is StaticExpectedValues)
                    outEdges.first { it.relationship.onInput.containsAll(onInput) }
                else
                    outEdges.first()
                graph.getEdgeTarget(edge)
            }
            else -> {
                log.trace("Handling a {} state", state::class.simpleName)
                graph.getEdgeTarget(graph.outgoingEdgesOf(node).first())
            }
        }

        log.trace("Root search is: {}", root)
        val iterator: BreadthFirstIterator<BotNode, BotEdge> = BreadthFirstIterator(graph, root)

        val l: MutableList<BotState> = mutableListOf()
        while (iterator.hasNext()) {
            val next: BotNode = iterator.next()
            log.trace("Next is: {}", next)
            l.add(next.state)

            if (next.state is WaitForInputState || next.state is EndState)
                break
        }

        return l.toList()
    }

    class BotEdge(val relationship: BotStateRelationship) : DefaultEdge() {
        override fun toString(): String = "${relationship.from}-${relationship.to}"
        override fun hashCode(): Int = toString().hashCode()
        override fun equals(other: Any?): Boolean = (other is BotEdge) && (this.toString() == other.toString())
    }

    class BotNode(val state: BotState) {
        override fun toString(): String = state.id
        override fun hashCode(): Int = toString().hashCode()
        override fun equals(other: Any?): Boolean = (other is BotNode) && (this.toString() == other.toString())
    }

    private val graph: Graph<BotNode, BotEdge>
    val startState: StartState
    val endState: EndState

    fun missingStartState(states: List<BotState>) {
        log.trace("Checking missing start state...")
        if (states.none { it.type == "start" }) {
            log.warn("Missing start state")
            throw BotConfigException("A bot configuration must have a start state")
        }
        log.trace("Start state is ok")
    }

    fun missingEndState(states: List<BotState>) {
        log.trace("Checking missing end state...")
        if (states.none { it.type == "end" }) {
            log.warn("Missing end state")
            throw BotConfigException("A bot configuration must have an end state")
        }
        log.trace("Missing end state is ok")
    }

    fun duplicatedStateKeys(states: List<BotState>) {
        log.trace("Checking duplicated id keys...")
        val duplicatedKeys: Map<String, Int> = states.groupingBy { it.id }.eachCount().filter { it.value > 1 }
        if (duplicatedKeys.isNotEmpty()) {
            log.warn("There are duplicated state ids: $duplicatedKeys")
            throw BotConfigException("State ids ${duplicatedKeys.keys.sorted()} are not unique")
        }
        log.trace("No duplicated id keys detected")
    }

    fun duplicatedRelationshipKeys(relationships: List<BotStateRelationship>) {
        log.trace("Checking duplicated relationships...")
        val duplicatedRelationships =
            relationships.groupingBy { "${it.from}-${it.to}" }.eachCount().filter { it.value > 1 }
        if (duplicatedRelationships.isNotEmpty()) {
            log.warn("There are duplicated relationships: $duplicatedRelationships")
            throw BotConfigException("Relationships ${duplicatedRelationships.keys.sorted()} are not unique")
        }
        log.trace("No duplicated relationships detected")
    }

    fun checkLoops(relationships: List<BotStateRelationship>) {
        log.trace("Checking state loops...")
        val loops = relationships.filter { it.from == it.to }
        if (loops.isNotEmpty()) {
            log.warn("A state can't be linked to itself")
            throw BotConfigException("No state can be linked to itself: ${loops.map { it.to }
                .sorted()}")
        }
        log.trace("No state loops detected")
    }

    fun checkDeclaredRelationshipIds(states: List<BotState>, relationships: List<BotStateRelationship>) {
        log.trace("Checking ids declared in states and relationships")
        val stateIds: Set<String> = states.map { it.id }.toSet()
        val relIds: Set<String> = relationships.flatMap { listOf(it.from, it.to) }.toSet()
        val delta = relIds.subtract(stateIds).toList().sorted()
        log.trace("Delta is: {}", delta)
        if (delta.isNotEmpty()) {
            log.warn("There are invalid state ids in relationships: {}", delta)
            throw BotConfigException("Relationships contain state ids $delta which are not defined state ids")
        }
        log.trace("Relationship ids check is ok")
    }

    fun graphStateConnectivity(graph: Graph<BotNode, BotEdge>) {
        val startNode = graph.vertexSet().first { it.state.type == "start" }
        val endNode = graph.vertexSet().first { it.state.type == "end" }

        log.trace("Checking states connectivity...")
        val connectivity: ConnectivityInspector<BotNode, BotEdge> = ConnectivityInspector(graph)
        if (!connectivity.isConnected) {
            val connectedSets = connectivity.connectedSets()
            log.debug("Connected sets: {}", connectedSets)
            log.warn("Some nodes are not connected in the main path. Detecting them...")

            if (!connectivity.pathExists(startNode, endNode)) {
                log.warn("A path from start to end node doesn't exist")
                throw BotConfigException("A path between ${startNode.state.id} and ${endNode.state.id} must exists")
            }

            val notConnectedNodes = connectedSets
                .asSequence()
                .filter { !it.containsAll(setOf(startNode, endNode)) }
                .flatten()
                .map { it.state.id }
                .toList()
                .sorted()
                .toList()
            throw BotConfigException("The following states are not connected with start or end state: $notConnectedNodes")
        }
        log.trace("States connectivity is ok")
    }

    fun startHasBeforeNode(graph: Graph<BotNode, BotEdge>) {
        val startNode = graph.vertexSet().first { it.state.type == "start" }

        log.trace("Checking if start has no before states")
        val startFirstPredecessors: MutableList<BotNode> = Graphs.predecessorListOf(graph, startNode)
        if (startFirstPredecessors.isNotEmpty()) {
            val reversed: EdgeReversedGraph<BotNode, BotEdge> = EdgeReversedGraph(graph)
            val breadthFirstIterator: BreadthFirstIterator<BotNode, BotEdge> = BreadthFirstIterator(reversed, startNode)

            val predecessors = mutableListOf<BotNode>()
            breadthFirstIterator.next() // skip first node - start state
            while (breadthFirstIterator.hasNext())
                predecessors.add(breadthFirstIterator.next())

            val preStartStates = predecessors.map { it.state.id }.sorted()
            log.warn("The following nodes appear before start state: {}", preStartStates)
            throw BotConfigException("Start state is not first state. States $preStartStates are before")
        }
        log.trace("Start state is ok")
    }

    fun endHasNoAfterNode(graph: Graph<BotNode, BotEdge>) {
        val endNode = graph.vertexSet().first { it.state.type == "end" }
        log.trace("Checking if end has no after states")
        val endSuccessors: MutableList<BotNode> = Graphs.successorListOf(graph, endNode)
        if (endSuccessors.isNotEmpty()) {
            val breadthFirstIterator: BreadthFirstIterator<BotNode, BotEdge> = BreadthFirstIterator(graph, endNode)

            val predecessors = mutableListOf<BotNode>()
            breadthFirstIterator.next() // skip first node - end state
            while (breadthFirstIterator.hasNext())
                predecessors.add(breadthFirstIterator.next())

            val postEndStates = predecessors.map { it.state.id }.sorted()
            log.warn("The following nodes appear after end state: {}", postEndStates)
            throw BotConfigException("End state is not last state. States $postEndStates are after")
        }
        log.trace("End state is ok")
    }

    fun checkWaitForInputExpectedValues(graph: Graph<BotNode, BotEdge>) {
        log.trace("Checking wait-for-input static expected values")
        val waitForInputNodes = graph.vertexSet()
            .filter { it.state is WaitForInputState && it.state.expectedValues is StaticExpectedValues }
        waitForInputNodes.forEach { wfi ->
            val state: WaitForInputState = wfi.state as WaitForInputState
            val expectedValues: Set<String> = (state.expectedValues as StaticExpectedValues).values.toSet()
            val outEdges: List<BotStateRelationship> = graph.outgoingEdgesOf(wfi).map { it.relationship }
            val onInputs: Set<String> = outEdges.flatMap { it.onInput }.toSet()

            log.trace("Checking {} expected values: {}", state.id, expectedValues)
            log.trace("Outgoing relationships: {}", onInputs)

            if (expectedValues != onInputs) {
                log.warn("Detected mismatch on ${state.id} state. expected-values: $expectedValues, on-inputs: $onInputs")
                val valuesOnlyInExpected: List<String> = expectedValues.subtract(onInputs).toList().sorted()
                if (valuesOnlyInExpected.isNotEmpty()) {
                    log.warn("There are some expected-values not mapped on on-inputs")
                    throw BotConfigException("Static input state '${state.id}' has no outgoing relationship on input $valuesOnlyInExpected")
                }

                val valuesOnlyOnInput: List<String> = onInputs.subtract(expectedValues).toList().sorted()
                if (valuesOnlyOnInput.isNotEmpty()) {
                    log.warn("There are some on-inputs declared not mapped in expected-values")
                    throw BotConfigException("Static input state '${state.id}' doesn't declare expected values $valuesOnlyOnInput but relationships from ${state.id} declares on-input $valuesOnlyOnInput")
                }
            }
        }
        log.trace("Static wait-for-input detected types ok")
    }

    init {
        log.trace("Running preliminary checks...")
        val statesCheck = listOf(
            ::missingStartState,
            ::missingEndState,
            ::duplicatedStateKeys
        )
        statesCheck.forEach { it(states) }

        val relationshipsCheck = listOf(
            ::duplicatedRelationshipKeys,
            ::checkLoops
        )
        relationshipsCheck.forEach { it(relationships) }

        val statesAndRelationshipsCheck = listOf(
            ::checkDeclaredRelationshipIds
        )
        statesAndRelationshipsCheck.forEach { it(states, relationships) }

        log.debug("Building internal graph...")
        val nodeMap: Map<String, BotNode> = states.map {
            it.id to BotNode(
                it
            )
        }.toMap()
        graph = DirectedMultigraph(BotEdge::class.java)

        states.forEach { graph.addVertex(BotNode(it)) }
        relationships.forEach {
            graph.addEdge(
                nodeMap[it.from], nodeMap[it.to],
                BotEdge(it)
            )
        }
        log.trace("Graph built: {}", graph)

        val startNode: BotNode = nodeMap.filter { it.value.state.type == "start" }.values.first()
        val endNode: BotNode = nodeMap.filter { it.value.state.type == "end" }.values.first()
        startState = startNode.state as StartState
        endState = endNode.state as EndState

        val graphChecks = listOf(
            ::graphStateConnectivity,
            ::startHasBeforeNode,
            ::endHasNoAfterNode,
            ::checkWaitForInputExpectedValues
        )
        graphChecks.forEach { it(graph) }
    }
}

