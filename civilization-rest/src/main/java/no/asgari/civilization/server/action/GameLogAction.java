package no.asgari.civilization.server.action;

import com.google.common.base.Preconditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j;
import no.asgari.civilization.server.application.CivSingleton;
import no.asgari.civilization.server.model.Draw;
import no.asgari.civilization.server.model.GameLog;
import no.asgari.civilization.server.model.Item;
import no.asgari.civilization.server.model.Player;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Action class responsible for logging private and public logs
 */
@Log4j
public class GameLogAction {
    private final JacksonDBCollection<GameLog, String> gameLogCollection;
    private final JacksonDBCollection<Player, String> playerCollection;

    public GameLogAction(DB db) {
        this.gameLogCollection = JacksonDBCollection.wrap(db.getCollection(GameLog.COL_NAME), GameLog.class, String.class);
        this.playerCollection = JacksonDBCollection.wrap(db.getCollection(Player.COL_NAME), Player.class, String.class);
    }

    String save(@NotNull @Valid GameLog gameLog) {
        Preconditions.checkNotNull(gameLog);

        WriteResult<GameLog, String> insert = this.gameLogCollection.insert(gameLog);
        log.debug("Saved Gamelog with _id " + insert.getSavedId());
        return insert.getSavedId();
    }

    public GameLog createGameLog(Draw draw, GameLog.LogType logType) {
        GameLog pl = new GameLog();
        pl.setDraw(draw);
        pl.setPbfId(draw.getPbfId());
        try {
            pl.setUsername(CivSingleton.instance().playerCache().get(draw.getPlayerId()));
        } catch (ExecutionException e) {
            log.error("Couldn't retrieve username from cache");
            pl.setUsername(getUsernameFromPlayerId(draw.getPlayerId()));
        }
        pl.createAndSetLog(logType);
        pl.setId(save(pl));
        return pl;
    }

    public GameLog createGameLog(Item item, String pbfId, GameLog.LogType logType) {
        return createGameLog(item, pbfId, logType, item.getOwnerId());
    }

    public GameLog createGameLog(Item item, String pbfId, GameLog.LogType logType, String playerId) {
        GameLog pl = new GameLog();
        Draw<Item> draw = new Draw<>(pbfId, item.getOwnerId());
        draw.setItem(item);
        pl.setDraw(draw);
        pl.setPbfId(pbfId);
        try {
            pl.setUsername(CivSingleton.instance().playerCache().get(playerId));
        } catch (ExecutionException e) {
            log.error("Couldn't retrieve username from cache");
            pl.setUsername(getUsernameFromPlayerId(playerId));
        }
        pl.createAndSetLog(logType);
        pl.setId(save(pl));
        return pl;
    }

    public GameLog createGameLog(Draw draw, String pbfId, String username, boolean vote) {
        GameLog pl = new GameLog();
        pl.setPbfId(pbfId);
        pl.setUsername(username);
        pl.setPublicLog(username + " has voted " + (vote ? "yes" : "no") + " to undo " + draw.getItem().getName());
        pl.setId(save(pl));
        return pl;
    }

    /**
     * Common messages like, user joined game, user withdrew game etc
     */
    public GameLog createCommonPublicLog(String publicMessage, String pbfId, String playerId) {
        GameLog pl = new GameLog();
        pl.setPbfId(pbfId);
        try {
            pl.setUsername(CivSingleton.instance().playerCache().get(playerId));
        } catch (Exception e) {
            log.error("Couldn't retrieve username from cache");
            pl.setUsername(getUsernameFromPlayerId(playerId));
        }
        pl.setPublicLog(pl.getUsername() + " " + publicMessage);
        pl.setPrivateLog("");
        pl.setId(save(pl));
        return pl;
    }

    public GameLog createCommonPrivateLog(String privateMessage, String pbfId, String playerId) {
        GameLog pl = new GameLog();
        pl.setPbfId(pbfId);
        try {
            pl.setUsername(CivSingleton.instance().playerCache().get(playerId));
        } catch (Exception e) {
            log.error("Couldn't retrieve username from cache");
            pl.setUsername(getUsernameFromPlayerId(playerId));
        }
        pl.setPrivateLog(pl.getUsername() + " " + privateMessage);
        pl.setPublicLog("");
        pl.setId(save(pl));
        return pl;
    }

    public GameLog createCommonPrivatePublicLog(String message, String pbfId, String playerId) {
        GameLog pl = new GameLog();
        pl.setPbfId(pbfId);
        try {
            pl.setUsername(CivSingleton.instance().playerCache().get(playerId));
        } catch (Exception e) {
            log.error("Couldn't retrieve username from cache");
            pl.setUsername(getUsernameFromPlayerId(playerId));
        }
        pl.setPrivateLog(pl.getUsername() + " " + message);
        pl.setPublicLog(pl.getUsername() + " " + message);
        pl.setId(save(pl));
        return pl;
    }

    public GameLog findGameLogById(String id) {
        return gameLogCollection.findOneById(id);
    }

    public WriteResult<GameLog, String> updateGameLogById(GameLog gameLog) {
        return gameLogCollection.updateById(gameLog.getId(), gameLog);
    }

    private String getUsernameFromPlayerId(String playerId) {
        return playerCollection.findOneById(playerId).getUsername();
    }

    public List<GameLog> getGameLogs(String pbfId) {
        @Cleanup DBCursor<GameLog> gameLogsCursor = gameLogCollection.find(DBQuery.is("pbfId", pbfId), new BasicDBObject());
        List<GameLog> gamelogs = new ArrayList<>(gameLogsCursor.size());
        while (gameLogsCursor.hasNext()) {
            gamelogs.add(gameLogsCursor.next());
        }
        return gamelogs;
    }


    public List<GameLog> getGameLogsBelongingToPlayer(String pbfId, String username) {
        @Cleanup DBCursor<GameLog> gameLogsCursor = gameLogCollection.find(DBQuery.is("pbfId", pbfId).is("username", username), new BasicDBObject());
        List<GameLog> gamelogs = new ArrayList<>(gameLogsCursor.size());
        while (gameLogsCursor.hasNext()) {
            gamelogs.add(gameLogsCursor.next());
        }
        return gamelogs;
    }

}
