package com.example.securenote.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    fun observeAll(): Flow<List<Note>> = dao.observeAll()
    suspend fun findById(id: Long): Note? = dao.findById(id)
    suspend fun save(note: Note): Long {
        val now = System.currentTimeMillis()
        return if (note.id == 0L) {
            dao.insert(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(note.copy(updatedAt = now))
            note.id
        }
    }
    suspend fun delete(note: Note) = dao.delete(note)
}
