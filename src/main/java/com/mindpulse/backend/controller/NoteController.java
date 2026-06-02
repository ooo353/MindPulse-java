package com.mindpulse.backend.controller;

import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.entity.Note;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.service.AiAgentClient;
import com.mindpulse.backend.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "笔记管理", description = "笔记CRUD及异步AI摘要生成接口")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private AiAgentClient aiAgentClient;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    @Operation(summary = "同步上传笔记", description = "上传笔记内容及可选附件，同步保存到数据库")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "笔记创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "创建失败")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNote(
            @Parameter(description = "笔记标题") @RequestParam("title") String title,
            @Parameter(description = "笔记内容") @RequestParam("content") String content,
            @Parameter(description = "标签，逗号分隔") @RequestParam(value = "tags", required = false) String tags,
            @Parameter(description = "附件文件") @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String username = getCurrentUsername();
            Note createdNote = noteService.uploadNote(title, content, username, tags, file);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "Note created successfully");
            data.put("note", createdNote);
            return ResponseEntity.status(201).body(ApiResponse.success(201, "Note created successfully", data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create note: " + e.getMessage()));
        }
    }

    @Operation(summary = "异步上传笔记并生成摘要", description = "上传笔记内容后立即返回，后台通过AI异步生成摘要和标签，处理完成后通过WebSocket实时推送结果")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "笔记已提交，异步处理中",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "提交失败")
    })
    @PostMapping(value = "/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNoteAsync(
            @Parameter(description = "笔记标题", required = true) @RequestParam("title") String title,
            @Parameter(description = "笔记内容", required = true) @RequestParam("content") String content,
            @Parameter(description = "标签，逗号分隔") @RequestParam(value = "tags", required = false) String tags,
            @Parameter(description = "附件文件") @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String username = getCurrentUsername();
            Map<String, Object> result = noteService.createNoteAsync(title, content, username, tags, file);

            return ResponseEntity.status(201).body(
                    ApiResponse.success(201, "笔记已提交，摘要异步处理中", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("提交笔记失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "查询所有笔记", description = "获取当前用户的所有笔记，支持关键字搜索")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Note>>> getAllNotes(
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        String username = getCurrentUsername();

        List<Note> notes;
        if (keyword != null && !keyword.isEmpty()) {
            notes = noteService.searchNotes(keyword, username);
        } else {
            notes = noteService.getAllNotesByUser(username);
        }

        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @Operation(summary = "根据ID查询笔记", description = "获取单条笔记详情，需验证所有权")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> getNoteById(
            @Parameter(description = "笔记ID") @PathVariable Long id) {
        try {
            Note note = noteService.getNoteById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

            String username = getCurrentUsername();
            if (!note.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this note"));
            }

            return ResponseEntity.ok(ApiResponse.success(note));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "更新笔记", description = "更新笔记的标题、内容、标签等信息，需验证所有权")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> updateNote(
            @Parameter(description = "笔记ID") @PathVariable Long id,
            @RequestBody NoteDto noteDto) {
        try {
            Note existingNote = noteService.getNoteById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

            String username = getCurrentUsername();
            if (!existingNote.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this note"));
            }

            NoteDto updatedNoteDto = new NoteDto(
                noteDto.id(),
                noteDto.title(),
                noteDto.content(),
                noteDto.type(),
                noteDto.fileUrl(),
                noteDto.tags(),
                noteDto.summary(),
                noteDto.category(),
                noteDto.status(),
                username
            );
            Note updatedNote = noteService.updateNote(id, updatedNoteDto);
            return ResponseEntity.ok(ApiResponse.success("Note updated successfully", updatedNote));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to update note: " + e.getMessage()));
        }
    }

    @Operation(summary = "删除笔记", description = "根据ID删除笔记，需验证所有权")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteNote(
            @Parameter(description = "笔记ID") @PathVariable Long id) {
        try {
            Note existingNote = noteService.getNoteById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

            String username = getCurrentUsername();
            if (!existingNote.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this note"));
            }

            noteService.deleteNote(id);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Note deleted successfully");
            return ResponseEntity.ok(ApiResponse.success("Note deleted successfully", data));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to delete note: " + e.getMessage()));
        }
    }

    @Operation(summary = "同步生成摘要（已废弃）", description = "该接口仅用于调试和向后兼容，建议使用 POST /api/notes/async 异步接口")
    @PostMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateSummary(
            @Parameter(description = "笔记ID") @PathVariable Long id) {
        try {
            Note note = noteService.getNoteById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

            String username = getCurrentUsername();
            if (!note.getAuthor().equals(username)) {
                return ResponseEntity.status(403).body(ApiResponse.forbidden("Access denied: You don't own this note"));
            }

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

            note = noteService.updateNote(id,
                    new NoteDto(note.getId(), note.getTitle(), note.getContent(),
                            note.getType(), note.getFileUrl(), note.getTags(),
                            note.getSummary(), note.getCategory(), note.getStatus(),
                            note.getAuthor()));

            Map<String, Object> data = new HashMap<>();
            data.put("summary", summaryResult.get("summary"));
            data.put("tags", summaryResult.get("tags"));
            data.put("updated_note", note);
            data.put("message", "Summary generated and note updated successfully");

            return ResponseEntity.ok(ApiResponse.success("Summary generated and note updated successfully", data));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to generate summary: " + e.getMessage()));
        }
    }
}
