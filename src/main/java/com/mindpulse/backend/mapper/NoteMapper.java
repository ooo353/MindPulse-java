package com.mindpulse.backend.mapper;

import com.mindpulse.backend.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper {
    Note findById(@Param("id") Long id);
    
    List<Note> findByAuthor(@Param("username") String username);
    
    List<Note> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
        @Param("keyword") String keyword, 
        @Param("username") String username
    );
    
    void insertNote(Note note);
    
    void updateNote(Note note);

    void updateSummaryAndTags(@Param("id") Long id,
                              @Param("title") String title,
                              @Param("summary") String summary,
                              @Param("tags") String tags,
                              @Param("category") String category,
                              @Param("status") String status);

    void deleteById(@Param("id") Long id);
}