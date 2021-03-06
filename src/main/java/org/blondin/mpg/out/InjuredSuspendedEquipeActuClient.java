package org.blondin.mpg.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.blondin.mpg.AbstractClient;
import org.blondin.mpg.config.Config;
import org.blondin.mpg.out.model.OutType;
import org.blondin.mpg.out.model.Player;
import org.blondin.mpg.out.model.Position;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * https://www.equipeactu.fr/blessures-et-suspensions/fodbold/
 */
public class InjuredSuspendedEquipeActuClient extends AbstractClient {

    private static final Map<String, String> TEAM_NAME_WRAPPER = new HashMap<>();
    private static final Map<String, String> LOGO_NAME_WRAPPER = new HashMap<>();

    private EnumMap<ChampionshipOutType, List<Player>> cache = new EnumMap<>(ChampionshipOutType.class);

    static {
        /*
         * Team name "EquipeActu -> MPG" wrapper (by championship)
         */

        // Ligue 1
        TEAM_NAME_WRAPPER.put("Amiens SC", "Amiens");
        TEAM_NAME_WRAPPER.put("Angers Sco", "Angers");
        TEAM_NAME_WRAPPER.put("Angers SCO", "Angers");
        TEAM_NAME_WRAPPER.put("Nimes", "Nîmes");
        TEAM_NAME_WRAPPER.put("PSG", "Paris");
        TEAM_NAME_WRAPPER.put("Olympique Lyon", "Lyon");
        TEAM_NAME_WRAPPER.put("Olympique Lyonnais", "Lyon");
        TEAM_NAME_WRAPPER.put("Olympique Marseille", "Marseille");
        TEAM_NAME_WRAPPER.put("Paris Saint Germain", TEAM_NAME_WRAPPER.get("PSG"));
        TEAM_NAME_WRAPPER.put("Saint Etienne", "Saint-Étienne");

        // Premiere League
        TEAM_NAME_WRAPPER.put("Afc Bournemouth", "Bournemouth");
        TEAM_NAME_WRAPPER.put("Brighton And Hove Albion", "Brighton");
        TEAM_NAME_WRAPPER.put("Manchester City", "Man. City");
        TEAM_NAME_WRAPPER.put("Leicester City", "Leicester");
        TEAM_NAME_WRAPPER.put("Manchester United", "Man. United");
        TEAM_NAME_WRAPPER.put("Newcastle United", "Newcastle");
        TEAM_NAME_WRAPPER.put("Norwich City", "Norwich");
        TEAM_NAME_WRAPPER.put("Sheffield United", "Sheffield");
        TEAM_NAME_WRAPPER.put("Sheffield U.", "Sheffield");
        TEAM_NAME_WRAPPER.put("West Ham United", "West Ham");
        TEAM_NAME_WRAPPER.put("Wolverhampton Wanderers", "Wolverhampton");

        // Serie A
        TEAM_NAME_WRAPPER.put("Bologne", "Bologna");
        TEAM_NAME_WRAPPER.put("AC Milan", "Milan");
        TEAM_NAME_WRAPPER.put("Rome", "Roma");
        TEAM_NAME_WRAPPER.put("SSC Napoli", "Napoli");
        TEAM_NAME_WRAPPER.put("SPAL 2013", "Spal");
        TEAM_NAME_WRAPPER.put("Internazionale", "Inter");
        TEAM_NAME_WRAPPER.put("SSD Parma", "Parma");

        // Ligua
        TEAM_NAME_WRAPPER.put("Alaves", "Alavés");
        TEAM_NAME_WRAPPER.put("Celta Vigo", "Celta");
        TEAM_NAME_WRAPPER.put("Celta De Vigo", "Celta");
        TEAM_NAME_WRAPPER.put("Deportivo Alavés", "Alavés");
        TEAM_NAME_WRAPPER.put("Athletic Bilbao", "Bilbao");
        TEAM_NAME_WRAPPER.put("Atletico Bilbao", "Bilbao");
        TEAM_NAME_WRAPPER.put("Atlético Madrid", "Atlético");
        TEAM_NAME_WRAPPER.put("Atletico Madrid", "Atlético");
        TEAM_NAME_WRAPPER.put("Barcelone", "Barcelona");
        TEAM_NAME_WRAPPER.put("Grenade", "Granada");
        TEAM_NAME_WRAPPER.put("Leganes", "Leganés");
        TEAM_NAME_WRAPPER.put("Majorque", "Mallorca");
        TEAM_NAME_WRAPPER.put("Séville", "Sevilla");
        TEAM_NAME_WRAPPER.put("Real Betis", "Betis");
        TEAM_NAME_WRAPPER.put("Real Valladolid", "Valladolid");
        TEAM_NAME_WRAPPER.put("Valence", "Valencia");
        TEAM_NAME_WRAPPER.put("Hellas Verona", "Verona");

        /*
         * Logo name wrapper
         */

        // Ligue 1
        LOGO_NAME_WRAPPER.put("Psg", TEAM_NAME_WRAPPER.get("PSG"));

        // Ligua
        LOGO_NAME_WRAPPER.put("Athletic Club", TEAM_NAME_WRAPPER.get("Atletico Bilbao"));

        // Serie A
        LOGO_NAME_WRAPPER.put("Internazionale", "Inter");
    }

    public static InjuredSuspendedEquipeActuClient build(Config config) {
        return build(config, null);
    }

    public static InjuredSuspendedEquipeActuClient build(Config config, String urlOverride) {
        InjuredSuspendedEquipeActuClient client = new InjuredSuspendedEquipeActuClient();
        client.setUrl(StringUtils.defaultString(urlOverride, "https://www.equipeactu.fr/blessures-et-suspensions/fodbold/"));
        client.setProxy(config.getProxy());
        client.setSslCertificatesCheck(config.isSslCertificatesCheck());
        return client;
    }

    /**
     * Return MPG team name from EquipeActu team name (because has change during time)
     * 
     * @param equipeActuTeamName The EquipeActu team name
     * @return The MPG team name
     */
    private static String getTeamName(String equipeActuTeamName) {
        if (TEAM_NAME_WRAPPER.containsKey(equipeActuTeamName)) {
            return TEAM_NAME_WRAPPER.get(equipeActuTeamName);
        }
        return equipeActuTeamName;
    }

    /**
     * Return injured or suspended player
     * 
     * @param championship Championship of player
     * @param playerName   Player Name
     * @param position     Position
     * @param teamName     Team Name
     * @return Player or null if not found
     */
    public Player getPlayer(ChampionshipOutType championship, String playerName, Position position, String teamName) {
        OutType[] excludes = null;
        return getPlayer(championship, playerName, position, teamName, excludes);
    }

    /**
     * Return injured or suspended player
     * 
     * @param championship Championship of player
     * @param playerName   Player Name
     * @param position     Position
     * @param teamName     Team Name
     * @param excludes     {@link OutType} to exclude
     * @return Player or null if not found
     */
    public Player getPlayer(ChampionshipOutType championship, String playerName, Position position, String teamName, OutType... excludes) {
        List<OutType> excluded = Arrays.asList(ObjectUtils.defaultIfNull(excludes, new OutType[] {}));

        for (Player player : getPlayers(championship)) {
            if (!excluded.contains(player.getOutType())
                    && Stream.of(playerName.toLowerCase().split(" ")).allMatch(player.getFullNameWithPosition().toLowerCase()::contains)) {
                Position pos = player.getPosition();
                if (Position.UNDEFINED.equals(pos) || Position.UNDEFINED.equals(position) || position.equals(pos)) {
                    if (StringUtils.isNotBlank(teamName) && StringUtils.isNotBlank(player.getTeam()) && !player.getTeam().equals(teamName)) {
                        continue;
                    }
                    return player;
                }
            }
        }
        return null;
    }

    public String getHtmlContent(ChampionshipOutType championship) {
        return get(championship.getValue(), String.class, TIME_HOUR_IN_MILLI_SECOND);
    }

    public List<Player> getPlayers(ChampionshipOutType championship) {
        if (!cache.containsKey(championship)) {
            cache.put(championship, getPlayers(getHtmlContent(championship)));
        }
        return cache.get(championship);
    }

    private List<Player> getPlayers(String content) {
        List<Player> players = new ArrayList<>();
        Document doc = Jsoup.parse(content);
        boolean oneTeamHasBeenParsed = false;
        String team = null;
        for (Element item : doc.select("div.injuries_item")) {
            // Retrieve team from name or logo
            team = parseTeam(item, team);
            if (StringUtils.isNoneBlank(team)) {
                oneTeamHasBeenParsed = true;
            }

            if (item.selectFirst("div.injuries_name") == null) {
                // No injured/suspended players in team
                continue;
            }

            Player player = new Player();
            player.setTeam(getTeamName(team));
            player.setOutType(parseOutType(item.selectFirst("div.injuries_type").selectFirst("span").className()));
            player.setFullNameWithPosition(item.selectFirst("div.injuries_playername").text());
            player.setDescription(item.selectFirst("div.injuries_name").text());
            player.setLength(item.selectFirst("div.injuries_length").text());
            players.add(player);
        }
        if (!oneTeamHasBeenParsed) {
            throw new UnsupportedOperationException("No teams have been found, parsing problem");
        }
        return players;
    }

    private static String parseTeam(Element item, String currentTeam) {
        String team = currentTeam;
        Element teamItem = item.parent().previousElementSibling();
        if (teamItem.selectFirst("img") != null) {
            String teamName = teamItem.text().trim();
            if (StringUtils.isNotBlank(teamName)) {
                team = teamName;
            } else {
                // No team name, get from png logo name
                String logoUrl = teamItem.selectFirst("img").attr("src");
                if (logoUrl.contains("blank_team")) {
                    // Some logo can be "blank_team" ... team set to blank in this case
                    team = null;
                } else {
                    team = logoUrl.substring(logoUrl.lastIndexOf('/') + 1, logoUrl.lastIndexOf(".png"));
                    team = WordUtils.capitalizeFully(team.replace('-', ' '));
                    if (LOGO_NAME_WRAPPER.containsKey(team)) {
                        team = LOGO_NAME_WRAPPER.get(team);
                    }
                }
            }
        }
        return team;
    }

    private static OutType parseOutType(String htmlContent) {
        final String prefix = "sitesprite icon_";
        if (!StringUtils.startsWith(htmlContent, prefix)) {
            throw new UnsupportedOperationException(String.format("HTML content should start with prefix '%s': %s ", prefix, htmlContent));
        }
        return OutType.getNameByValue(htmlContent.substring(prefix.length()));
    }

}
