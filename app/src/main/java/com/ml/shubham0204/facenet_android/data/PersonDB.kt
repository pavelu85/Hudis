package com.ml.shubham0204.facenet_android.data

import io.objectbox.kotlin.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class PersonDB {
    private val personBox = ObjectBoxStore.store.boxFor(PersonRecord::class.java)

    fun addPerson(person: PersonRecord): Long = personBox.put(person)

    fun removePerson(personID: Long) {
        personBox.removeByIds(listOf(personID))
    }

    fun updateLastSeen(personID: Long, timestamp: Long) {
        val person = personBox.get(personID) ?: return
        personBox.put(person.copy(lastSeenTime = timestamp))
    }

    fun getById(personID: Long): PersonRecord? = personBox.get(personID)

    // Returns the number of records present in the collection
    fun getCount(): Long = personBox.count()

    // Returns all person records as a plain list (for one-shot reads, e.g. during recognition)
    fun getAllList(): List<PersonRecord> = personBox.all

    // Returns how many PersonRecords have names starting with "Unknown ".
    // Used to assign sequential auto-generated names in Auto-Monitor mode.
    fun countUnknownPersons(): Int =
        personBox.query(PersonRecord_.personName.startsWith("Unknown "))
            .build().count().toInt()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAll(): Flow<List<PersonRecord>> =
        personBox
            .query(PersonRecord_.personID.notNull())
            .build()
            .flow()
            .map { it.toList() }   // defensive copy — ObjectBox may reuse the MutableList internally
            .flowOn(Dispatchers.IO)
}
