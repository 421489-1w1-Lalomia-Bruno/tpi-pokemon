package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="sync_metadata")
@Getter
@Setter
@NoArgsConstructor
public class SyncMetaDataEntity extends BaseEntity {
    @Column(name = "set_id", nullable = false, unique = true, length = 20)
    private String setId;

    @Column(name = "last_sync_at", nullable = false)
    private LocalDateTime lastSyncAt;

    @Column(name = "synced_count", nullable = false)
    private int syncedCount;
}
