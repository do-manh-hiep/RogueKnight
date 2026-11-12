package rogueknight.data;

import rogueknight.data.dto.PieceStatsData;

public class PieceSpec {
    private final int hp;
    private final Integer damage, cycle, swordDamage, captureDamage;

    private PieceSpec(
        int hp,
        Integer damage,
        Integer cycle,
        Integer swordDamage,
        Integer captureDamage
    ){
        this.hp = hp;
        this.damage = damage;
        this.cycle = cycle;
        this.swordDamage = swordDamage;
        this.captureDamage = captureDamage;
    }

    public static PieceSpec from(PieceStatsData stats){
        return new PieceSpec(
                stats.hp(),
                stats.damage(),
                stats.cycle(),
                stats.swordDamage(),
                stats.captureDamage()
        );
    }

    public int hp() { return hp; }
    public Integer damage() { return damage; }
    public Integer cycle() { return cycle; }
    public Integer swordDamage() { return swordDamage; }
    public Integer captureDamage() { return captureDamage; }
}
