package no.asgari.civilization.server.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.asgari.civilization.server.SheetName;

@Getter
@Setter
@JsonTypeName("infantry")
@NoArgsConstructor
public class Infantry implements Unit {
    private int level = LEVEL_1;
    private int attack;
    private int health;
    private String owner;
    private boolean hidden;
    private boolean used;
    private boolean dead;

    public Infantry(int attack, int health) {
        this.attack = attack;
        this.health = health;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return "Infantry";
    }

    @Override
    public SheetName getSheetName() {
        return SheetName.INFANTRY;
    }

    @Override
    public String toString() {
        switch (level) {
            case LEVEL_1:
                return "Spearmen " + attack + "." + health;
            case LEVEL_2:
                return "Pikemen " + (attack + LEVEL_1) + "." + (health + LEVEL_1);
            case LEVEL_3:
                return "Riflemen " + (attack + LEVEL_2) + "." + (health + LEVEL_2);
            case LEVEL_4:
                return "Modern Infantry" + (attack + LEVEL_3) + "." + (health + LEVEL_3);
            default:
                return "Unknown level" + attack + "." + health;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Infantry infantry = (Infantry) o;

        if (attack != infantry.attack) return false;
        if (dead != infantry.dead) return false;
        if (health != infantry.health) return false;
        if (hidden != infantry.hidden) return false;
        if (used != infantry.used) return false;
        if (owner != null ? !owner.equals(infantry.owner) : infantry.owner != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = health;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (hidden ? 1 : 0);
        result = 31 * result + (used ? 1 : 0);
        result = 31 * result + (dead ? 1 : 0);
        result = 31 * result + attack;

        return result;
    }
}