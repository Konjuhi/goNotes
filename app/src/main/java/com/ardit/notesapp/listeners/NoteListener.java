package com.ardit.notesapp.listeners;

import com.ardit.notesapp.entities.Note;

public interface NoteListener {
    void onNoteClicked(Note note,int position);
}
