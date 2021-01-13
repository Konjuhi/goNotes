package com.ardit.notesapp.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.ardit.notesapp.R;
import com.ardit.notesapp.adapter.NotesAdapter;
import com.ardit.notesapp.database.NotesDatabase;
import com.ardit.notesapp.entities.Note;
import com.ardit.notesapp.listeners.NoteListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteListener {

    //this request code is used to add a new note
    private static final int REQUEST_CODE_ADD_NOTE =1;
    //this request code is to used to update note
    private static final int REQUEST_CODE_UPDATE_NOTE =2;
    //this request is used to to display all notes
    public static final int REQUEST_CODE_SHOW_NOTES =3;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    ImageView imageAddNoteMain;

    private int noteClickedPosition=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                new Intent(getApplicationContext(), CreateNoteActivity.class),
                REQUEST_CODE_ADD_NOTE
                );
            }
        });

        notesRecyclerView = (RecyclerView)findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        //notesAdapter = new NotesAdapter(noteList);
        notesAdapter = new NotesAdapter(noteList,this);
        notesRecyclerView.setAdapter(notesAdapter);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(noteList.size() !=0){
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });

        //getNotes();

        //while this method is in onCreate() it means the application is just started and we need to display all notes from db
        getNotes(REQUEST_CODE_SHOW_NOTES,false);

    }

    //after we add  public static final int REQUEST_CODE_SHOW_NOTES =3, we add in method a parameter for getting the request code
   // private void getNotes(){ 2) we check if we have deleted note so we add a boolean
   private void getNotes(final int requestCode,final boolean isNoteDeleted){

        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void,Void, List<Note>>{

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                Log.d("MY_NOTES",notes.toString());

                //We checked if note list is empty , we are adding all notes from database to this note list and notify adapter about
                //new dataset,in another case if list is not empty then it means notes are already loaded from database so we are
                // just adding only the latest note to the note list and notify adapter about new note inserted
                //And last we scrolled our recycler view to the top

              /*  if(noteList.size() == 0){
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }else{
                    noteList.add(0,notes.get(0));
                    notesAdapter.notifyDataSetChanged();
                }
                notesRecyclerView.smoothScrollToPosition(0);*/

              //Now here we delete from if(noteList.size() == 0) to notesRecylerView....
                //here, request code is REQUEST_CODE_SHOW_NOTES,
                //so we are adding all notes from database to noteList and notify adapter for new data set

                if(requestCode == REQUEST_CODE_SHOW_NOTES){
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }else if(requestCode == REQUEST_CODE_ADD_NOTE){
                    //To notify adapter for new the newly inserted item and scrolling recycler view to the top
                    noteList.add(0,notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    notesRecyclerView.smoothScrollToPosition(0);
                }else if(requestCode == REQUEST_CODE_UPDATE_NOTE){
                    //When we remove note from clicked position and adding the latest updated note from the same position from db
                    //and notify adapter for item changed at the position
                    noteList.remove(noteClickedPosition);
                    /*noteList.add(noteClickedPosition,notes.get(noteClickedPosition));
                    notesAdapter.notifyItemInserted(noteClickedPosition);*/

                    if(isNoteDeleted){
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    }else{
                        noteList.add(noteClickedPosition,notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }

            }
        }
        new GetNotesTask().execute();
    }

    //Since we have started "CreateNoteActivity" for the result, we neet to handle the result in "onActivityResult"
    //method to update the note list after adding a note from "CreateNoteActivity"

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode == RESULT_OK){
           // getNotes();

            //getNotes() method is called from onActivityResult() method of activity and we checked the current
            // request code if for add note and the result is RESULT_OK.It means a new note is added from
            // CreateNote activity and its result is sent back to this activity that's why we are
            //passing REQUEST_CODE_ADD_NOTE to that method
            getNotes(REQUEST_CODE_SHOW_NOTES,false);
        }else if( requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode ==RESULT_OK){
            if(data !=null){
                //when already available note is updated from CreateNote and its result is sent back to activity
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate",true);
        intent.putExtra("note",note);
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE);
    }
}