package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import com.mindpulse.backend.entity.Note;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService implements INoteService {

    private final NoteMapper noteMapper;
    private final NoteSummaryProducer noteSummaryProducer;
    private final AiAgentClient aiAgentClient;

    @Value("${mindpulse.upload.path:./uploads}")
    private String uploadPath;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notes", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Note createNote(NoteDto noteDto) {
        Note note = new Note();
        note.setTitle(noteDto.title());
        note.setContent(noteDto.content());
        note.setType(noteDto.type());
        note.setTags(noteDto.tags());
        note.setSummary(noteDto.summary());
        note.setCategory(noteDto.category());
        note.setStatus(noteDto.status() != null ? noteDto.status() : "processing");
        note.setAuthor(noteDto.author());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        noteMapper.insertNote(note);
        return note;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notes", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Note uploadNote(String title, String content, String author, String tags, MultipartFile file) throws IOException {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setAuthor(author);
        note.setTags(tags);
        note.setStatus("processing");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String filePath = uploadPath + File.separator + fileName;

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(file.getBytes());
            }

            note.setFileUrl(filePath);
            note.setType(determineFileType(file.getOriginalFilename()));
        }

        noteMapper.insertNote(note);
        return note;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notes", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Map<String, Object> createNoteAsync(String title, String content, String author,
                                                String tags, MultipartFile file) throws IOException {
        Note note = uploadNote(title, content, author, tags, file);
        log.info("Note saved: noteId={}, submitting summary task to message queue", note.getId());

        NoteSummaryMessage message = new NoteSummaryMessage(
                note.getId(), note.getContent(), note.getTitle(), author
        );
        noteSummaryProducer.sendSummaryTask(message);

        Map<String, Object> result = new HashMap<>();
        result.put("noteId", note.getId());
        result.put("status", "processing");
        result.put("message", "Note submitted, summary processing asynchronously");
        result.put("note", note);
        return result;
    }

    @Override
    @Cacheable(value = "notes", key = "'author_' + #username")
    public List<Note> getAllNotesByUser(String username) {
        return noteMapper.findByAuthor(username);
    }

    @Override
    @Cacheable(value = "notes", key = "'search_' + #keyword + '_' + #username")
    public List<Note> searchNotes(String keyword, String username) {
        return noteMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, username);
    }

    @Override
    @Cacheable(value = "notes", key = "#id")
    public Optional<Note> getNoteById(Long id) {
        Note note = noteMapper.findById(id);
        return note != null ? Optional.of(note) : Optional.empty();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notes", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public Note updateNote(Long id, NoteDto noteDto) {
        Note existingNote = noteMapper.findById(id);

        if (existingNote != null) {
            existingNote.setTitle(noteDto.title());
            existingNote.setContent(noteDto.content());
            existingNote.setType(noteDto.type());
            existingNote.setTags(noteDto.tags());
            existingNote.setSummary(noteDto.summary());
            existingNote.setCategory(noteDto.category());
            existingNote.setStatus(noteDto.status());
            existingNote.setUpdatedAt(LocalDateTime.now());

            noteMapper.updateNote(existingNote);
            return existingNote;
        } else {
            throw new RuntimeException("Note not found with id: " + id);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "notes", allEntries = true),
        @CacheEvict(value = "dashboard", allEntries = true)
    })
    public void deleteNote(Long id) {
        noteMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> generateSummary(Long noteId, String username) {
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found with id: " + noteId);
        }
        verifyOwnership(note.getAuthor(), username, noteId);

        Map<String, Object> summaryResult = aiAgentClient.generateSummary(note.getContent());

        String newTitle = (String) summaryResult.get("title");
        if (newTitle != null && !newTitle.isEmpty() && !newTitle.equals("Auto-generated Title")) {
            note.setTitle(newTitle);
        }

        String newTags = (String) summaryResult.get("tags");
        if (newTags != null && !newTags.isEmpty()) {
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                note.setTags(note.getTags() + "," + newTags);
            } else {
                note.setTags(newTags);
            }
        }

        note = updateNote(noteId,
                new NoteDto(note.getId(), note.getTitle(), note.getContent(),
                        note.getType(), note.getFileUrl(), note.getTags(),
                        note.getSummary(), note.getCategory(), note.getStatus(),
                        note.getAuthor()));

        Map<String, Object> data = new HashMap<>();
        data.put("summary", summaryResult.get("summary"));
        data.put("tags", summaryResult.get("tags"));
        data.put("updated_note", note);
        data.put("message", "Summary generated and note updated successfully");
        return data;
    }

    @Override
    public void verifyOwnership(String entityAuthor, String currentUser, Long entityId) {
        if (!entityAuthor.equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to access entity with id " + entityId);
        }
    }

    private String determineFileType(String fileName) {
        if (fileName == null) return "text";
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot < 0) return "text";
        String extension = fileName.substring(lastDot + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "pdf";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return "image";
            default:
                return "text";
        }
    }
}
