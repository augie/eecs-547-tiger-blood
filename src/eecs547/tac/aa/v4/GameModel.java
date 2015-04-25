package eecs547.tac.aa.v4;

import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import java.util.LinkedHashMap;

/**
 *
 * @author Augie
 */
public class GameModel {

    protected TigerBloodV4 agent;
    private LinkedHashMap<String, OpponentModel> opponents = new LinkedHashMap<String, OpponentModel>();
    public UserModel uM = new UserModel(this);

    public GameModel(TigerBloodV4 agent) {
        this.agent = agent;
    }

    public OpponentModel getOpponentModel(String opponent) {
        if (!opponents.containsKey(opponent)) {
            opponents.put(opponent, new OpponentModel(opponent, this));
        }
        return opponents.get(opponent);
    }

    public void handleQueryReport(QueryReport queryReport) {
        for (Query q : agent.querySpace) {
            for (String advertiser : queryReport.advertisers(q)) {
                getOpponentModel(advertiser).handleQueryReport(q, queryReport);
            }
        }
        uM.handleQueryReport(queryReport);
    }
}
