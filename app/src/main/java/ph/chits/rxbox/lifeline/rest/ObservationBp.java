package ph.chits.rxbox.lifeline.rest;

import java.util.ArrayList;
import java.util.List;

public class ObservationBp {
    String resourceType = "Observation";
    String id;
    String status = "final";
    Code code;
    Subject subject;
    String effectiveDateTime;
    List<Component> component;

    public ObservationBp(String resourceType, String id, String status, Coding coding, String subject, String effectiveDateTime, int systolic, int diastolic, int map) {
        this.resourceType = resourceType;
        this.id = id;
        this.status = status;

        this.code = new Code();
        this.code.coding = new ArrayList<>(1);
        this.code.coding.add(coding);

        this.subject = new Subject();
        this.subject.reference = subject;

        this.effectiveDateTime = effectiveDateTime;

        component = new ArrayList<>(3);
        component.add(new Component(
                new Coding("8480-6", "http://loinc.org"),
                new Component.ValueQuantity((float) systolic, "mm[Hg]", "http://unitsofmeasure.org", "mmHg")
        ));
        component.add(new Component(
                new Coding("8462-4", "http://loinc.org"),
                new Component.ValueQuantity((float) diastolic, "mm[Hg]", "http://unitsofmeasure.org", "mmHg")
        ));
        component.add(new Component(
                new Coding("8478-0", "http://loinc.org"),
                new Component.ValueQuantity((float) map, "mm[Hg]", "http://unitsofmeasure.org", "mmHg")
        ));

    }

    public static class Code {
        List<Coding> coding;
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

    public static class Component {
        Code code;
        ValueQuantity valueQuantity;

        public Component(Coding coding, ValueQuantity valueQuantity) {
            this.code = new Code();
            this.code.coding = new ArrayList<>(1);
            this.code.coding.add(coding);
            this.valueQuantity = valueQuantity;
        }

        public static class ValueQuantity {
            public ValueQuantity() {
            }

            public ValueQuantity(Float value, String code, String system, String unit) {
                this.value = value;
                this.code = code;
                this.system = system;
                this.unit = unit;
            }

            Float value;
            String code, system, unit;
        }
    }
}
