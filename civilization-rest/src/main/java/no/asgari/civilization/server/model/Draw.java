package no.asgari.civilization.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * This class is used in GameLog to show what item have been drawn
 * <p/>
 * It will support undo of draws, which will put the item back in the deck and shuffle
 * Each draw will have a collection of Undo, which contains information about a possible undo with votes and outcome
 * <p/>
 * <T> - Typically implementation of Unit, Item, or Tech
 */
@NoArgsConstructor
@JsonRootName(value = "draw")
@Data
//TODO Draw is not a good name really. Its really more a UndoableItem
public class Draw<T extends Item> {
    public static final String COL_NAME = "draw";

    /**
     * Typically implementation of Unit or Item. Should have #getSheetName() to determine the type
     */
    @NotNull
    private T item;

    public Draw(String pbfId, String playerId) {
        this.pbfId = pbfId;
        this.playerId = playerId;

        created = LocalDateTime.now();
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime created;

    /**
     * The user that made the draw. Its always a player that initiates a draw, so this cannot be blank.
     */
    @NotBlank
    private String playerId;
    /**
     * A draw must always belong to a game. The pbf game id
     */
    @NotBlank
    private String pbfId;

    /**
     * If null, then no undo has been performed
     */
    private Undo undo = null;

    /**
     * Returns true if undo has been requested
     */
    @JsonIgnore
    private boolean isUndoInitiated() {
        return undo != null;
    }

}
