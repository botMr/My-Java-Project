public class Rank {
    private final String country;
    private final String happinessRank;

    public Rank(String country, String happinessRank) {
        this.country = country;
        this.happinessRank = happinessRank;
    }

    public String getCountry() {
        return country;
    }

    public String getHappinessRank() {
        return happinessRank;
    }
}
