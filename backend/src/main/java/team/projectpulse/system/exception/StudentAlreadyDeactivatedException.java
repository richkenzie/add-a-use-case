package team.projectpulse.system.exception;

/**
 * A deactivate request named a student who is already deactivated (UC-STU-deactivate-student, extension 6a).
 * Mapped to {@code 409 CONFLICT} by {@link ExceptionHandlerAdvice}.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("This student is already deactivated.");
    }

}
