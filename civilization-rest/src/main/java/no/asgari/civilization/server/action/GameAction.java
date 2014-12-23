package no.asgari.civilization.server.action;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j;
import no.asgari.civilization.server.application.CivSingleton;
import no.asgari.civilization.server.dto.*;
import no.asgari.civilization.server.excel.ItemReader;
import no.asgari.civilization.server.exception.PlayerExistException;
import no.asgari.civilization.server.model.*;
import no.asgari.civilization.server.util.Java8Util;
import no.asgari.civilization.server.util.SecurityCheck;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Log4j
public class GameAction extends BaseAction {
    private final JacksonDBCollection<PBF, String> pbfCollection;
    private final JacksonDBCollection<Player, String> playerCollection;
    private final GameLogAction gameLogAction;

    public GameAction(DB db) {
        super(db);
        this.playerCollection = JacksonDBCollection.wrap(db.getCollection(Player.COL_NAME), Player.class, String.class);
        this.pbfCollection = JacksonDBCollection.wrap(db.getCollection(PBF.COL_NAME), PBF.class, String.class);
        this.gameLogAction = new GameLogAction(db);
    }

    public String createNewGame(CreateNewGameDTO dto, String playerId) {
        PBF pbf = new PBF();
        pbf.setName(dto.getName());
        pbf.setType(dto.getType());
        pbf.setNumOfPlayers(dto.getNumOfPlayers());
        if (CivSingleton.instance().itemsCache() == null) {
            CivSingleton.instance().setItemsCache(
                    CacheBuilder.newBuilder()
                            .maximumSize(4) //1 for each game type
                            .removalListener(lis -> log.debug("Removing " + lis.getKey() + " from the gameCache"))
                            .build(new CacheLoader<GameType, ItemReader>() {
                                public ItemReader load(GameType type) {
                                    ItemReader itemReader = new ItemReader();
                                    try {
                                        itemReader.readItemsFromExcel(dto.getType());
                                    } catch (IOException e) {
                                        log.error("Couldn't read from Excel file " + e.getMessage(), e);
                                        throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
                                    }
                                    return itemReader;
                                }
                            })
            );
        }

        ItemReader itemReader;
        try {
            itemReader = CivSingleton.instance().itemsCache().get(dto.getType());
        } catch (ExecutionException e) {
            log.error("Couldnt get itemReader from cache " + e.getMessage(), e);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        pbf.getItems().addAll(itemReader.shuffledCivs);
        pbf.getItems().addAll(itemReader.shuffledCultureI);
        pbf.getItems().addAll(itemReader.shuffledCultureII);
        pbf.getItems().addAll(itemReader.shuffledCultureIII);
        pbf.getItems().addAll(itemReader.shuffledGPs);
        pbf.getItems().addAll(itemReader.shuffledHuts);
        pbf.getItems().addAll(itemReader.shuffledVillages);
        pbf.getItems().addAll(itemReader.shuffledTiles);
        pbf.getItems().addAll(itemReader.shuffledCityStates);
        pbf.getItems().addAll(itemReader.ancientWonders);
        pbf.getItems().addAll(itemReader.medievalWonders);
        pbf.getItems().addAll(itemReader.modernWonders);
        pbf.getItems().addAll(itemReader.mountedList);
        pbf.getItems().addAll(itemReader.aircraftList);
        pbf.getItems().addAll(itemReader.artilleryList);
        pbf.getItems().addAll(itemReader.infantryList);
        pbf.getTechs().addAll(itemReader.allTechs);

        WriteResult<PBF, String> pbfInsert = pbfCollection.insert(pbf);
        pbf.setId(pbfInsert.getSavedId());
        log.info("PBF game created with id " + pbfInsert.getSavedId());

        log.info("Join the game created");
        joinGame(pbf.getId(), playerId);
        return pbf.getId();
    }

    public List<PbfDTO> getAllActiveGames() {
        @Cleanup DBCursor<PBF> dbCursor = pbfCollection.find(DBQuery.is("active", true), new BasicDBObject());
        return Java8Util.streamFromIterable(dbCursor)
                .map(GameAction::createPbfDTO).collect(Collectors.toList());
    }

    /**
     * Creating PbfDTO so to not include every players Ghand and information
     *
     * @param pbf - the PBF
     * @return PbfDto
     */
    private static PbfDTO createPbfDTO(PBF pbf) {
        PbfDTO dto = new PbfDTO();
        dto.setType(pbf.getType());
        dto.setId(pbf.getId());
        dto.setName(pbf.getName());
        dto.setActive(pbf.isActive());
        long created = pbf.getCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        dto.setCreated(created);
        dto.setNumOfPlayers(pbf.getNumOfPlayers());
        dto.setPlayers(pbf.getPlayers().stream()
                .map(p -> createPlayerDTO(p, pbf.getId()))
                .collect(Collectors.toList()));
        return dto;
    }

    private static PlayerDTO createPlayerDTO(Playerhand player, String pbfId) {
        PlayerDTO dto = new PlayerDTO();
        dto.setUsername(player.getUsername());
        dto.setPlayerId(player.getPlayerId());
        dto.setPbfId(pbfId);
        return dto;
    }

    private static Playerhand createPlayerHand(Player player) {
        Playerhand playerhand = new Playerhand();
        playerhand.setUsername(player.getUsername());
        playerhand.setPlayerId(player.getId());
        playerhand.setYourTurn(false);
        return playerhand;
    }

    /**
     * Joins a game. If it is full it will throw exception
     */
    public void joinGame(String pbfId, String playerId) {
        PBF pbf = pbfCollection.findOneById(pbfId);
        if (pbf.getNumOfPlayers() == pbf.getPlayers().size()) {
            log.warn("Cannot join the game. Its full");
            Response badReq = Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot join the game. Its full.")
                    .build();
            throw new WebApplicationException(badReq);
        }

        Player player = playerCollection.findOneById(playerId);
        player.getGameIds().add(pbfId);
        playerCollection.updateById(player.getId(), player);

        Playerhand playerhand = createPlayerHand(player);
        if (!pbf.getPlayers().contains(playerhand)) {
            pbf.getPlayers().add(playerhand);
        }
        startIfAllPlayers(pbf);
        pbfCollection.updateById(pbf.getId(), pbf);
    }

    /**
     * Returns the username of the starting turn player
     *
     * @param pbfId
     * @return
     */
    public String getTurnPlayer(String pbfId) {
        PBF pbf = findPBFById(pbfId);
        return pbf.getPlayers().stream()
                .filter(Playerhand::isYourTurn)
                .findFirst()
                .orElseThrow(PlayerAction::cannotFindPlayer)
                .getUsername();

    }

    public List<PlayerDTO> getAllPlayers(String pbfId) {
        Preconditions.checkNotNull(pbfId);
        PBF pbf = pbfCollection.findOneById(pbfId);
        return pbf.getPlayers().stream()
                .map(p -> createPlayerDTO(p, pbf.getId()))
                .collect(Collectors.toList());
    }

    private void startIfAllPlayers(PBF pbf) {
        final int numOfPlayersNeeded = pbf.getNumOfPlayers();
        if (numOfPlayersNeeded == pbf.getPlayers().size()) {
            Playerhand randomPlayer = getRandomPlayer(pbf.getPlayers());
            log.debug("Setting starting player " + randomPlayer);
            randomPlayer.setYourTurn(true);
            createInfoLog(pbf.getId(), "Starting player is " + randomPlayer.getUsername());
            pbfCollection.updateById(pbf.getId(), pbf);
        }
    }

    private Playerhand getRandomPlayer(List<Playerhand> players) {
        Collections.shuffle(players);
        return players.get(0);
    }

    //TODO test it
    public GameDTO getGame(PBF pbf, Player player) {
        //Set common stuff
        GameDTO dto = new GameDTO();
        long created = pbf.getCreated().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        dto.setCreated(created);
        dto.setId(pbf.getId());
        dto.setType(pbf.getType());
        dto.setName(pbf.getName());

        //Set logs
        List<GameLog> allPublicLogs = gameLogAction.getAllPublicLogs(pbf.getId());
        List<GameLogDTO> publicGamelogDTOs = allPublicLogs.stream()
                .map(log -> new GameLogDTO(log.getId(), log.getPublicLog(), log.getCreatedInMillis()))
                .collect(Collectors.toList());
        dto.setPublicLogs(publicGamelogDTOs);

        //Set private player info if correct player is loggedIn.
        if (player != null && !Strings.isNullOrEmpty(player.getUsername()) && !Strings.isNullOrEmpty(player.getId())) {
            //Get all the private player stuff
            Optional<Playerhand> playerhand = pbf.getPlayers()
                    .stream()
                    .filter(p -> p.getPlayerId().equals(player.getId()))
                    .findFirst();

            if (playerhand.isPresent()) {
                List<GameLog> allPrivateLogs = gameLogAction.getAllPrivateLogs(pbf.getId(), playerhand.get().getUsername());
                List<GameLogDTO> privateGamelogDTOs = allPrivateLogs.stream()
                        .map(log -> new GameLogDTO(log.getId(), log.getPublicLog(), log.getCreatedInMillis()))
                        .collect(Collectors.toList());

                dto.setPlayer(playerhand.get());
                dto.setPrivateLogs(privateGamelogDTOs);
            }
        }
        return dto;
    }

    public boolean withdrawFromGame(String pbfId, String playerId) {
        PBF pbf = pbfCollection.findOneById(pbfId);
        if (pbf.getNumOfPlayers() == pbf.getPlayers().size()) {
            return false;
        }

        if (!SecurityCheck.hasUserAcces(pbf, playerId)) {
            log.warn("User with id " + playerId + " is not player this game, and cannot withdraw");
            Response badReq = Response.status(Response.Status.FORBIDDEN)
                    .entity("No access")
                    .build();
            throw new WebApplicationException(badReq);
        }

        Iterator<Playerhand> iterator = pbf.getPlayers().iterator();
        while(iterator.hasNext()) {
            Playerhand playerhand = iterator.next();
            if(playerhand.getPlayerId().equals(playerId)) {
                iterator.remove();
                //TOOD Create log player withdrew from game
                gameLogAction.createCommonPublicLog("Withdrew from join", pbfId, playerId);
                pbfCollection.updateById(pbf.getId(), pbf);
                return true;
            }
        }


        return false;
    }
}
