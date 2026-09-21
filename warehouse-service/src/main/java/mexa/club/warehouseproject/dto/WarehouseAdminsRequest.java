package mexa.club.warehouseproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class WarehouseAdminsRequest {

    /**
     * Ombor adminlari — {@code users.id}. Bo‘sh ro‘yxat — barcha biriktirishlarni olib tashlash.
     */
    @NotNull
    private List<UUID> adminUserIds = new ArrayList<>();
}
