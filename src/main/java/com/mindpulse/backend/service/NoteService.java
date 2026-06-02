package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import com.mindpulse.backend.entity.Note;
import com.mindpulse.backend.mapper.NoteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
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

@Service
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteSummaryProducer noteSummaryProducer;

    @CacheEvict(value = "notes", key = "#result.id")
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
            String filePath = "uploads/" + fileName;

            File uploadDir = new File("uploads/");
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

    /**
     * 异步上传笔记：保存后投递到 RabbitMQ，立即返回"处理中"状态
     */
    public Map<String, Object> createNoteAsync(String title, String content, String author,
                                                String tags, MultipartFile file) throws IOException {
        Note note = uploadNote(title, content, author, tags, file);
        log.info("笔记已保存: noteId={}, 投递摘要任务到消息队列", note.getId());

        // 投递到 RabbitMQ 异步处理
        NoteSummaryMessage message = new NoteSummaryMessage(
                note.getId(), note.getContent(), note.getTitle(), author
        );
        noteSummaryProducer.sendSummaryTask(message);

        Map<String, Object> result = new HashMap<>();
        result.put("noteId", note.getId());
        result.put("status", "processing");
        result.put("message", "笔记已提交，摘要处理中");
        result.put("note", note);
        return result;
    }

    @Cacheable(value = "notes", key = "'author_' + #username")
    public List<Note> getAllNotesByUser(String username) {
        return noteMapper.findByAuthor(username);
    }

    @Cacheable(value = "notes", key = "'search_' + #keyword")
    public List<Note> searchNotes(String keyword, String username) {
        return noteMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, username);
    }

    @Cacheable(value = "notes", key = "#id")
    public Optional<Note> getNoteById(Long id) {
        Note note = noteMapper.findById(id);
        return note != null ? Optional.of(note) : Optional.empty();
    }

    @CacheEvict(value = "notes", key = "#id")
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

    @CacheEvict(value = "notes", key = "#id")
    public void deleteNote(Long id) {
        noteMapper.deleteById(id);
    }

    private String determineFileType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
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