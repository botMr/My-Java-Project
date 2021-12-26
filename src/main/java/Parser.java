import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import javax.swing.JFrame;

import java.awt.*;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Scanner;

public class Parser
{
    public static void main(String[] args) throws SQLException {
        Scanner input = new Scanner(System.in);
        System.out.println("Нужно парсить csv? + или - : ");
        var str = input.next();

        Statistic countryStatistic;

        if (str.equals("+")) {
            countryStatistic = new Statistic(Paths.get("Показатель счастья по странам 2015.csv").toAbsolutePath());
            DatabaseSave(countryStatistic);
        }

        System.out.println("\nЗадача 1\n");

        Task1();

        System.out.println("\nЗадача 2");

        Task2("Latin America and Caribbean");
        Task2("Eastern Asia");

        System.out.println("\nЗадача 3");

        Task3("Western Europe");
        Task3("North America");
    }

    public static void Task3(String region) throws SQLException {
        String query = String.format("""
                SELECT region.country, region.region, ABS(scores.happinessScore - avg) as abs
                                FROM region
                                LEFT JOIN scores ON scores.country = region.country
                                LEFT JOIN (SELECT AVG(scores.happinessScore) as avg, region.region as reg
                                    FROM scores
                                    LEFT JOIN region ON region.country = scores.country
                                    WHERE reg = '%s')
                                WHERE region = '%s'
                                ORDER BY abs
                                LIMIT 1""", region, region);

        var rowSet = MakeExecuteQuery(query);
        assert rowSet != null;
        rowSet.next();
        System.out.println("\nОтвет: " + rowSet.getString(1) + "\n");
        PrintCachedRowSet(rowSet);
    }

    public static void Task2(String region) throws SQLException {
        String query = String.format("""
                SELECT scores.country, scores.economy, region.region
                                FROM scores
                                LEFT JOIN region ON region.country = scores.country
                                WHERE region = '%s'
                                ORDER BY economy DESC
                                LIMIT 1""", region);

        var rowSet = MakeExecuteQuery(query);
        assert rowSet != null;
        rowSet.next();
        System.out.println("\nОтвет: " + rowSet.getString(1) + "\n");
        PrintCachedRowSet(rowSet);

    }
    public static void Task1() throws SQLException {
        var rowSet = MakeExecuteQuery("SELECT country, economy FROM scores");
        JFreeChart chart = ChartFactory.createBarChart(
                "График по показателю экономики объеденый по странам",
                null,
                "Экономика",
                GetDataSetForDiagrams(rowSet));

        chart.setBackgroundPaint(Color.blue);

        JFrame frame = new JFrame("Economy");
        frame.getContentPane().add(new ChartPanel(chart));
        frame.setSize(600,400);
        frame.setVisible(true);
        PrintCachedRowSet(rowSet);
    }

    private static void PrintCachedRowSet(CachedRowSet rowSet) throws SQLException {
        var columnNames = GetColumnNames(rowSet);
        rowSet.beforeFirst();
        for (var i = 1; rowSet.next(); i++) {
            var sb = new StringBuilder();
            var str1 = i + ". ";
            sb.append(str1);
            for (var j = 1; j <= columnNames.size(); j++) {
                var str2 = "\"" + columnNames.get(j - 1) + "\"" + ":" + rowSet.getString(j) + "; ";
                sb.append(str2);
            }
            sb.deleteCharAt(sb.length() - 1);
            System.out.println(sb);
        }
    }

    private static ArrayList<String> GetColumnNames(CachedRowSet rowSet) throws SQLException {
        var metaData = rowSet.getMetaData();
        var result = new ArrayList<String>();
        rowSet.beforeFirst();
        rowSet.next();
        for (var i = 1; i <= metaData.getColumnCount(); i++) {
            result.add(metaData.getColumnName(i));
        }
        return result;
    }

    public static CategoryDataset GetDataSetForDiagrams(CachedRowSet rowSet) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try {
            while (rowSet.next()) {
                dataset.addValue(rowSet.getDouble(2), rowSet.getString(1), "Регионы");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataset;
    }

    public static CachedRowSet MakeExecuteQuery(String query) {
        Connection connection = null;
        Statement statement = null;

        try {
            RowSetFactory factory = RowSetProvider.newFactory();
            CachedRowSet rowSet = factory.createCachedRowSet();

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:country-statistic.sqlite");
            statement = connection.createStatement();

            var resSet = statement.executeQuery(query);
            rowSet.populate(resSet);

            return rowSet;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert statement != null;
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void DatabaseSave(Statistic countryStatistic) {
        Connection connection = null;
        Statement regionStatement = null;
        Statement rankStatement = null;
        Statement scoresStatement = null;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:country-statistic.sqlite");
            regionStatement = connection.createStatement();
            rankStatement = connection.createStatement();
            scoresStatement = connection.createStatement();

            for (var i: countryStatistic.getRegions()) {
                regionStatement.executeUpdate(String.format("INSERT INTO region values ('%s', '%s')",
                        i.getCountry(),
                        i.getRegion()));
            }

            for (var i: countryStatistic.getRanks()) {
                rankStatement.executeUpdate(String.format("INSERT INTO rank values ('%s', '%s')",
                        i.getCountry(),
                        i.getHappinessRank()));
            }

            for (var i: countryStatistic.getScores()) {
                scoresStatement.executeUpdate(String.format("INSERT INTO scores values ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')",
                        i.getCountry(),
                        i.getHappinessScore(),
                        i.getStandardError(),
                        i.getEconomy(),
                        i.getFamily(),
                        i.getHealth(),
                        i.getFreedom(),
                        i.getTrust(),
                        i.getGenerosity(),
                        i.getDystopiaResidual()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert regionStatement != null;
                regionStatement.close();
                assert rankStatement != null;
                rankStatement.close();
                assert scoresStatement != null;
                scoresStatement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
