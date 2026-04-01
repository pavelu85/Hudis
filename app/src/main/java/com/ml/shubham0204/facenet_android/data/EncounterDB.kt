package com.ml.shubham0204.facenet_android.data

import org.koin.core.annotation.Single

@Single
class EncounterDB {

    private val box = ObjectBoxStore.store.boxFor(EncounterRecord::class.java)

    fun addEncounter(personID: Long, lat: Double, lon: Double, source: String, locationName: String = "") {
        box.put(
            EncounterRecord(
                personID = personID,
                latitude = lat,
                longitude = lon,
                timestamp = System.currentTimeMillis(),
                source = source,
                locationName = locationName,
            )
        )
        pruneToLatestFive(personID)
    }

    fun getLastFive(personID: Long): List<EncounterRecord> =
        box.query(EncounterRecord_.personID.equal(personID))
            .orderDesc(EncounterRecord_.timestamp)
            .build()
            .find(0, 5)

    fun removeAllForPerson(personID: Long) {
        val ids = box.query(EncounterRecord_.personID.equal(personID))
            .build().findIds()
        box.removeByIds(ids.toList())
    }

    private fun pruneToLatestFive(personID: Long) {
        val all = box.query(EncounterRecord_.personID.equal(personID))
            .orderDesc(EncounterRecord_.timestamp)
            .build()
            .find()
        if (all.size > 5) {
            box.removeByIds(all.drop(5).map { it.id })
        }
    }
}
