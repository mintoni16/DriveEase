package driveease;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CoreConceptsTest {
    @Test
    void emailIsTrimmedAndNormalized() {
        assertEquals("student@example.com", Validate.cleanEmail(" Student@Example.com "));
    }

    @Test
    void invalidEmailBecomesValidationException() {
        ApiException ex = assertThrows(ApiException.class, () -> Validate.cleanEmail("not-an-email"));
        assertEquals(400, ex.status);
        assertInstanceOf(ValidationException.class, ex);
    }

    @Test
    void carInheritsCommonVehicleProperties() {
        Vehicle vehicle = new Models.Car(1, "Test Car", "Sedan", "A test vehicle", 5,
                "Manual", "Petrol", 1000, false);
        assertEquals("Test Car", vehicle.name());
        assertEquals("Sedan", vehicle.category());
        assertInstanceOf(Models.Car.class, vehicle);
    }

    @Test
    void carProducesFrontendJsonFields() {
        Models.Car car = new Models.Car(7, "Test Car", "Sedan", "A test vehicle", 5,
                "Manual", "Petrol", 1000, true);
        Map<String, Object> json = car.toMap();
        assertEquals(7, json.get("id"));
        assertEquals(1000, json.get("pricePerDay"));
        assertTrue((Boolean) json.get("popular"));
    }

    @Test
    void integerInputRejectsFractions() {
        assertEquals(3, Validate.toInt(3.0, "id required"));
        assertThrows(ApiException.class, () -> Validate.toInt(3.5, "id required"));
    }
}
