package com.benhsoan.persistence.mapper.queue;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.Room;
import com.benhsoan.persistence.entity.queue.DoctorRoomAssignmentEntity;
import com.benhsoan.persistence.entity.queue.QueueItemEntity;
import com.benhsoan.persistence.entity.queue.RoomEntity;

@Component
public class QueueStructurePersistenceMapper {
    public Room toDomain(RoomEntity entity) { return entity == null ? null : Room.restore(entity.getId(), entity.getCode(), entity.getName(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt()); }
    public DoctorRoomAssignment toDomain(DoctorRoomAssignmentEntity entity) { return entity == null ? null : DoctorRoomAssignment.restore(entity.getId(), entity.getDoctorId(), entity.getRoomId(), entity.getAssignedBy(), entity.getAssignedAt()); }
    public QueueItem toDomain(QueueItemEntity entity) { return entity == null ? null : QueueItem.restore(entity.getId(), entity.getMedicalQueueId(), entity.getPatientId(), entity.getAppointmentId(), entity.getVisitId(), entity.getSourceType(), entity.getStatus(), entity.getQueueNumber(), entity.getQueueDate(), entity.getCheckedInAt(), entity.getCalledAt(), entity.getCompletedAt(), entity.getCancelledAt(), entity.getCancelReason(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()); }
}
