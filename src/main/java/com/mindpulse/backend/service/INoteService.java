package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.entity.Note;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface INoteService {
    Note createNote(NoteDto noteDto);
    Note uploadNote(String title, String content, String author, String tags, MultipartFile file) throws IOException;
    Map<String, Object> createNoteAsync(String title, String content, String author, String tags, MultipartFile file) throws IOException;
    List<Note> getAllNotesByUser(String username);
    List<Note> searchNotes(String keyword, String username);
    Optional<Note> getNoteById(Long id);
    Note updateNote(Long id, NoteDto noteDto);
    void deleteNote(Long id);
    Map<String, Object> generateSummary(Long noteId, String username);
    void verifyOwnership(String entityAuthor, String currentUser, Long entityId);
}
