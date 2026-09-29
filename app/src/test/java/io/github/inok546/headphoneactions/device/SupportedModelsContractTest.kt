// SPDX-License-Identifier: AGPL-3.0-only

package io.github.inok546.headphoneactions.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariants every model in [supportedModels] must keep, checked for all models at once so that
 * a newly added one is covered automatically. Action IDs are persisted by Samsung Routines (as
 * shortcut IDs) and by widget configurations, so collisions or dangling IDs break users' setups.
 */
class SupportedModelsContractTest {

    @Test
    fun `there is at least one supported model`() {
        assertFalse(supportedModels.isEmpty())
    }

    @Test
    fun `model IDs are set and unique`() {
        supportedModels.forEach { assertTrue("Blank model ID in ${it.displayName}", it.id.isNotBlank()) }
        assertNoDuplicates("model IDs", supportedModels.map { it.id })
    }

    @Test
    fun `action IDs are set, unique within their model and scoped to it`() {
        supportedModels.forEach { model ->
            model.actions.forEach { action ->
                assertTrue("Blank action ID in ${model.id}", action.id.isNotBlank())
                assertTrue(
                    "${action.id} does not start with \"${model.id}.\"",
                    action.id.startsWith("${model.id}.") && action.id.length > model.id.length + 1,
                )
            }
            assertNoDuplicates("action IDs of ${model.id}", model.actions.map { it.id })
        }
    }

    @Test
    fun `action IDs are unique across all models`() {
        assertNoDuplicates("action IDs across models", supportedModels.flatMap { model -> model.actions.map { it.id } })
    }

    @Test
    fun `actions have a label and a short label`() {
        supportedModels.flatMap { it.actions }.forEach { action ->
            assertTrue("Blank label of ${action.id}", action.label.isNotBlank())
            assertTrue("Blank short label of ${action.id}", action.shortLabel.isNotBlank())
        }
    }

    @Test
    fun `quick actions are unique actions of their model`() {
        supportedModels.forEach { model ->
            assertNoDuplicates("quick action IDs of ${model.id}", model.quickActionIds)
            val actionIds = model.actions.map { it.id }.toSet()
            model.quickActionIds.forEach { id ->
                assertTrue("Quick action $id is not an action of ${model.id}", id in actionIds)
            }
        }
    }

    private fun assertNoDuplicates(what: String, ids: List<String>) {
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals("Duplicate $what", emptySet<String>(), duplicates)
    }
}
