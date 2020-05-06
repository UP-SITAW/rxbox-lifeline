package ph.chits.rxbox.lifeline.rest;

import java.util.ArrayList;
import java.util.List;

public class PatientQueue {
    String id, type;
    List<BackboneElement> member;

    public static class BackboneElement {
        Reference entity;
    }

    public static class Reference {
        String reference;
    }

    public List<String> getPatientIds() {
        List<String> ids = new ArrayList<>();
        for (BackboneElement e : member) {
            ids.add(e.entity.reference.split("/")[1]);
        }

        return ids;
    }
}
