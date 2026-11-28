package rogueknight.data.dto;

/** Optional balance values for one piece type. Nullable values represent absent JSON properties. */
public record PieceStatsData(
        Integer hp,
        Integer damage,
        Integer cycle,
        Integer swordDamage,
        Integer captureDamage) { }
