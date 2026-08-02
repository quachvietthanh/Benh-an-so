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
    public QueueItem toDomain(QueueItemEntity entity) { return entity == null ? null : QueueItem.restore(entity.getId(), entity.getMedicalQueueId(), entity.getPatientId(), entity.getAppointmentId(), entity.getVisitId(), entity.getSourceType(), entity.getStatus(), entity.getQueueNumber(), entity.getQueueDate(), entity.getCheckedInAt(), entity.getCalledAt(), entity.getCompletedAt(), entity.getCancelledAt(), entity.getCancelReason(), entity.getSkippedAt(), entity.getSkipReason(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt()); }
    public QueueItemEntity toEntity(QueueItem domain) {
        if (domain == null) return null;
        QueueItemEntity entity = new QueueItemEntity();
        entity.setId(domain.getId()); entity.setMedicalQueueId(domain.getMedicalQueueId()); entity.setPatientId(domain.getPatientId());
        entity.setAppointmentId(domain.getAppointmentId()); entity.setVisitId(domain.getVisitId()); entity.setSourceType(domain.getSourceType());
        entity.setStatus(domain.getStatus()); entity.setQueueNumber(domain.getQueueNumber()); entity.setQueueDate(domain.getQueueDate());
        entity.setCheckedInAt(domain.getCheckedInAt()); entity.setCalledAt(domain.getCalledAt()); entity.setCompletedAt(domain.getCompletedAt());
        entity.setCancelledAt(domain.getCancelledAt()); entity.setCancelReason(domain.getCancelReason()); entity.setCreatedBy(domain.getCreatedBy());
        entity.setSkippedAt(domain.getSkippedAt()); entity.setSkipReason(domain.getSkipReason());
        entity.setCreatedAt(domain.getCreatedAt()); entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
