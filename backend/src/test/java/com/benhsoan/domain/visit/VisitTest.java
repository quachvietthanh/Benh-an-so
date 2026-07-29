package com.benhsoan.domain.visit;
import static org.junit.jupiter.api.Assertions.*; import java.time.Instant; import java.util.UUID; import org.junit.jupiter.api.Test; import com.benhsoan.domain.shared.exception.ValidationException; import com.benhsoan.domain.visit.enums.*; import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
class VisitTest { private final Instant now=Instant.parse("2026-01-01T00:00:00Z"); private Visit visit(){return Visit.create("V001",UUID.randomUUID(),UUID.randomUUID(),null,null,VisitType.WALK_IN,now,"Reason",null,UUID.randomUUID());}
 @Test void followsLifecycle(){Visit v=visit();v.start(now);v.waitForResult(now.plusSeconds(1));v.resume(now.plusSeconds(2));v.complete(now.plusSeconds(3));assertTrue(v.isCompleted());}
 @Test void rejectsCompletionBeforeStart(){Visit v=visit();v.start(now);assertThrows(ValidationException.class,()->v.complete(now.minusSeconds(1)));}
 @Test void rejectsRegistrationChangeAfterStart(){Visit v=visit();v.start(now);assertThrows(VisitInvalidStatusException.class,()->v.updateRegistrationInformation(UUID.randomUUID(),null,null,VisitType.FOLLOW_UP,now,"R",null,now));}
}
