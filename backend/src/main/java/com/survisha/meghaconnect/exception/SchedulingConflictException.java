package com.survisha.meghaconnect.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when a scheduling conflict is detected.
 * Occurs when trying to schedule an appointment at a time that conflicts with existing schedule.
 */
public class SchedulingConflictException extends MeghaConnectException {
    
    private final LocalDateTime conflictDateTime;

    public SchedulingConflictException(LocalDateTime conflictDateTime) {
        super(ErrorCodeConstants.SCHEDULING_CONFLICT, 
              String.format(ErrorCodeConstants.SCHEDULING_CONFLICT_MSG, conflictDateTime), 
              409);
        this.conflictDateTime = conflictDateTime;
    }

    public SchedulingConflictException(String message, LocalDateTime conflictDateTime) {
        super(ErrorCodeConstants.SCHEDULING_CONFLICT, message, 409);
        this.conflictDateTime = conflictDateTime;
    }

    public SchedulingConflictException(String message, LocalDateTime conflictDateTime, Throwable cause) {
        super(ErrorCodeConstants.SCHEDULING_CONFLICT, message, 409, cause);
        this.conflictDateTime = conflictDateTime;
    }

    public LocalDateTime getConflictDateTime() {
        return conflictDateTime;
    }
}
