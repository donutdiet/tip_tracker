package com.example.tiptracker.data.model

import com.example.tiptracker.utils.collapseWhitespace

/** How many logs at one place use a name. */
data class NameCount(val name: String, val count: Int)

/**
 * A place linked to logs has one name across all of them. When a log named [typed] is saved at
 * a place whose other logs are named [others] and they differ, these are the names to choose
 * from: [typed] first, then the others, most used first, each with how many logs use it (the
 * saved log included). Names only differ if they still do once spacing is tidied; case and
 * punctuation count.
 *
 * Null when there's nothing to choose: the names all match, or [typed] is blank (a blank name
 * never renames a place).
 */
fun placeNameChoices(others: List<String>, typed: String): List<NameCount>? {
    val name = typed.collapseWhitespace()
    if (name.isEmpty()) return null
    val counts = LinkedHashMap<String, Int>()
    counts[name] = 1
    others
        .map { it.collapseWhitespace() }
        .filter { it.isNotEmpty() }
        .forEach { counts.merge(it, 1, Int::plus) }
    if (counts.size < 2) return null
    val (typedEntry, rest) = counts.entries.partition { it.key == name }
    return (typedEntry + rest.sortedByDescending { it.value }).map { NameCount(it.key, it.value) }
}
