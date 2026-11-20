package rogueknight.data.dto;

public class PieceStatsData {
    private final Integer hp, damage, cycle, swordDamage, captureDamage;

    public PieceStatsData(
        Integer hp,
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

    public Integer hp() { return hp; }
    public Integer damage() { return damage; }
    public Integer cycle() { return cycle; }
    public Integer swordDamage() { return swordDamage; }
    public Integer captureDamage() { return captureDamage; }
}
