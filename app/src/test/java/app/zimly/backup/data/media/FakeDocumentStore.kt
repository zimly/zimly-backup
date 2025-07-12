package app.zimly.backup.data.media

import app.zimly.backup.data.media.FakeDocumentsProvider.FakeDocument

class FakeDocumentStore {

    private var documentStore: MutableMap<String, FakeDocument> = mutableMapOf()

    fun add(vararg documents: FakeDocument) {
        documentStore.putAll(documents.associateBy { it.id })
    }

    operator fun get(id: String): FakeDocument? = documentStore[id]

    fun list(): MutableCollection<FakeDocument> = documentStore.values
}