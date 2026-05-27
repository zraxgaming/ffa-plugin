package xyz.zcraft.studios.zffa.profile;

public final class EloCalculator {
    private static final int K_FACTOR = 32;

    private EloCalculator() {
    }

    public static int calculateEloChange(int winnerElo, int loserElo) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        return Math.max(1, (int) Math.round(K_FACTOR * (1.0 - expectedScore)));
    }

    public static String rank(int elo) {
        if (elo < 900) return "Coal I";
        if (elo < 1000) return "Coal II";
        if (elo < 1100) return "Coal III";
        if (elo < 1250) return "Iron";
        if (elo < 1450) return "Gold";
        if (elo < 1700) return "Diamond";
        if (elo < 2000) return "Emerald";
        return "Netherstar";
    }
}
