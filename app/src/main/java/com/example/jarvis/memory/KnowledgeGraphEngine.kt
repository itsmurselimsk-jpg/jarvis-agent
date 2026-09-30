package com.example.jarvis.memory

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Associative Knowledge Graph & Long-Term Memory (inspired by isair/jarvis memory/graph.py)
 * 
 * Extracts facts, user traits, preferences, routines, and entity relationships
 * across conversational turns and provides associative retrieval for context injection.
 */
object KnowledgeGraphEngine {

    private const val TAG = "KnowledgeGraphEngine"
    private const val GRAPH_FILE_NAME = "jarvis_knowledge_graph.json"

    data class ConceptNode(
        val id: String,
        val label: String,
        val category: String, // "UserTrait", "Preference", "Project", "Contact", "Routine", "Fact"
        val value: String,
        val confidence: Double = 1.0,
        val lastUpdated: Long = System.currentTimeMillis()
    )

    data class KnowledgeEdge(
        val sourceId: String,
        val relation: String, // "likes", "works_on", "uses", "lives_in", "associated_with"
        val targetId: String,
        val weight: Double = 1.0
    )

    private val _nodes = MutableStateFlow<List<ConceptNode>>(emptyList())
    val nodes: StateFlow<List<ConceptNode>> = _nodes.asStateFlow()

    private val _edges = MutableStateFlow<List<KnowledgeEdge>>(emptyList())
    val edges: StateFlow<List<KnowledgeEdge>> = _edges.asStateFlow()

    private var storageFile: File? = null

    fun initialize(context: Context) {
        storageFile = File(context.filesDir, GRAPH_FILE_NAME)
        loadGraphFromDisk()

        // Seed with primary operator node if empty
        if (_nodes.value.isEmpty()) {
            addNode(ConceptNode("operator", "Primary Operator", "UserTrait", "User", 1.0))
        }
    }

    /**
     * Inspects a user utterance and automatically extracts relational facts.
     */
    fun extractFactsFromConversation(userInput: String) {
        val text = userInput.trim()
        val lower = text.lowercase(Locale.ROOT)

        // 1. Name extraction
        val nameRegex = Regex("""\b(?:my name is|mera naam|amar naam|call me|i am)\s+([A-Za-z]+)\b""", RegexOption.IGNORE_CASE)
        nameRegex.find(text)?.let {
            val name = it.groupValues[1]
            if (name.length > 1 && !listOf("not", "here", "ready", "fine", "ok").contains(name.lowercase())) {
                addFact("user_name", "Operator Name", "UserTrait", name, "identified_as")
            }
        }

        // 2. Preferences (Like / Love)
        val likeRegex = Regex("""\b(?:i like|i love|mujhe pasand hai|amar pochondo|favorite)\s+([A-Za-z0-9\s]{3,25})\b""", RegexOption.IGNORE_CASE)
        likeRegex.find(text)?.let {
            val target = it.groupValues[1].trim()
            addFact("pref_${target.hashCode()}", target, "Preference", target, "likes")
        }

        // 3. Projects & Work
        val projectRegex = Regex("""\b(?:working on|project|banara hoon|bana raha hoon|build korchhi)\s+([A-Za-z0-9\s]{3,30})\b""", RegexOption.IGNORE_CASE)
        projectRegex.find(text)?.let {
            val proj = it.groupValues[1].trim()
            addFact("proj_${proj.hashCode()}", proj, "Project", proj, "works_on")
        }

        // 4. Routine / Habit
        val habitRegex = Regex("""\b(?:every day|daily|roj|protidin)\s+([A-Za-z0-9\s]{3,30})\b""", RegexOption.IGNORE_CASE)
        habitRegex.find(text)?.let {
            val habit = it.groupValues[1].trim()
            addFact("routine_${habit.hashCode()}", habit, "Routine", habit, "routinely_does")
        }

        persistGraphToDisk()
    }

    fun addFact(id: String, label: String, category: String, value: String, relation: String) {
        val node = ConceptNode(id, label, category, value)
        addNode(node)
        addEdge(KnowledgeEdge("operator", relation, id, 1.0))
    }

    fun addNode(node: ConceptNode) {
        val current = _nodes.value.toMutableList()
        val index = current.indexOfFirst { it.id == node.id }
        if (index >= 0) {
            current[index] = node
        } else {
            current.add(node)
        }
        _nodes.value = current
        persistGraphToDisk()
    }

    fun addEdge(edge: KnowledgeEdge) {
        val current = _edges.value.toMutableList()
        val index = current.indexOfFirst { it.sourceId == edge.sourceId && it.relation == edge.relation && it.targetId == edge.targetId }
        if (index >= 0) {
            current[index] = edge
        } else {
            current.add(edge)
        }
        _edges.value = current
        persistGraphToDisk()
    }

    /**
     * Builds a structured memory digest of relevant facts based on query keywords.
     */
    fun getRelevantKnowledgeDigest(query: String): String? {
        val currentNodes = _nodes.value.filter { it.id != "operator" }
        if (currentNodes.isEmpty()) return null

        val queryTokens = query.lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        val relevantNodes = currentNodes.filter { node ->
            val nodeText = "${node.label} ${node.value} ${node.category}".lowercase(Locale.ROOT)
            queryTokens.any { token -> nodeText.contains(token) }
        }

        val targetList = if (relevantNodes.isNotEmpty()) relevantNodes else currentNodes.take(4)
        if (targetList.isEmpty()) return null

        val sb = StringBuilder()
        sb.appendLine("PERSISTENT KNOWLEDGE GRAPH (Extracted from past interactions):")
        targetList.forEach { node ->
            sb.appendLine("- [${node.category}] ${node.label}: ${node.value}")
        }
        return sb.toString().trim()
    }

    private fun persistGraphToDisk() {
        val file = storageFile ?: return
        try {
            val root = JSONObject()
            val nodesArr = JSONArray()
            _nodes.value.forEach { n ->
                nodesArr.put(JSONObject().apply {
                    put("id", n.id)
                    put("label", n.label)
                    put("category", n.category)
                    put("value", n.value)
                    put("confidence", n.confidence)
                    put("lastUpdated", n.lastUpdated)
                })
            }
            val edgesArr = JSONArray()
            _edges.value.forEach { e ->
                edgesArr.put(JSONObject().apply {
                    put("sourceId", e.sourceId)
                    put("relation", e.relation)
                    put("targetId", e.targetId)
                    put("weight", e.weight)
                })
            }
            root.put("nodes", nodesArr)
            root.put("edges", edgesArr)
            file.writeText(root.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist knowledge graph", e)
        }
    }

    private fun loadGraphFromDisk() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val nodesList = mutableListOf<ConceptNode>()
            val edgesList = mutableListOf<KnowledgeEdge>()

            val nodesArr = root.optJSONArray("nodes")
            if (nodesArr != null) {
                for (i in 0 until nodesArr.length()) {
                    val obj = nodesArr.getJSONObject(i)
                    nodesList.add(
                        ConceptNode(
                            id = obj.getString("id"),
                            label = obj.getString("label"),
                            category = obj.getString("category"),
                            value = obj.getString("value"),
                            confidence = obj.optDouble("confidence", 1.0),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }

            val edgesArr = root.optJSONArray("edges")
            if (edgesArr != null) {
                for (i in 0 until edgesArr.length()) {
                    val obj = edgesArr.getJSONObject(i)
                    edgesList.add(
                        KnowledgeEdge(
                            sourceId = obj.getString("sourceId"),
                            relation = obj.getString("relation"),
                            targetId = obj.getString("targetId"),
                            weight = obj.optDouble("weight", 1.0)
                        )
                    )
                }
            }

            _nodes.value = nodesList
            _edges.value = edgesList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load knowledge graph from disk", e)
        }
    }
}
