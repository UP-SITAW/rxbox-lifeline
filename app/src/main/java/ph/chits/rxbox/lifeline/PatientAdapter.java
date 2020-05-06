package ph.chits.rxbox.lifeline;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ph.chits.rxbox.lifeline.model.Patient;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {
    private List<Patient> patients;
    private int selected = -1;

    public PatientAdapter(List<Patient> patients) {
        this.patients = patients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View view = inflater.inflate(R.layout.item_patient, parent, false);
        ViewHolder holder = new ViewHolder(view);
        //view.setOnClickListener(holder);

        // Return a new holder instance
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data model based on position
        Patient patient = patients.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.name;
        textView.setText(patient.getName());

        holder.radio.setChecked(position == selected);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView name;
        public RadioButton radio;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            name = itemView.findViewById(R.id.name);
            radio = itemView.findViewById(R.id.radio);

            itemView.findViewById(R.id.root).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int old = selected;
            selected = getAdapterPosition();
            PatientAdapter.this.notifyItemChanged(old);
            PatientAdapter.this.notifyItemChanged(selected);
        }
    }

    public int getSelected() {
        return selected;
    }

    public void clearSelected() {
        selected = -1;
    }
}
