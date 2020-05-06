package ph.chits.rxbox.lifeline.rest;

public class ObservationQuantity {
    String resourceType = "Observation";
    String id;
    String status = "final";
    Code code;
    Subject subject;
    String effectiveDateTime;
    ValueQuantity valueQuantity;

    public ObservationQuantity() {
    }

    public ObservationQuantity(String resourceType, String id, String status, Coding coding, String subject, String effectiveDateTime, ValueQuantity valueQuantity) {
        this.resourceType = resourceType;
        this.id = id;
        this.status = status;

        this.code = new Code();
        this.code.coding = coding;

        this.subject = new Subject();
        this.subject.reference = subject;

        this.effectiveDateTime = effectiveDateTime;
        this.valueQuantity = valueQuantity;
    }

    public class Code {
        Coding coding;
    }

    public static class Coding {
        String code, system;

        public Coding() {
        }

        public Coding(String code, String system) {
            this.code = code;
            this.system = system;
        }
    }

    public class Subject {
        String reference;
    }

    public static class ValueQuantity {
        public ValueQuantity() {
        }

        public ValueQuantity(Float value, String code, String system) {
            this.value = value;
            this.code = code;
            this.system = system;
        }

        Float value;
        String code, system;
    }
}
